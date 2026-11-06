package dio.budgeting.application;

import dio.budgeting.application.port.TokenProvider;
import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.AuthenticationFailedException;
import dio.budgeting.domain.BusinessRuleViolationException;
import dio.budgeting.domain.ConflictException;
import dio.budgeting.domain.LoginBlockedException;
import dio.budgeting.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthUseCasesTest {

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final TokenProvider tokens = (userId, accountId) ->
            new TokenProvider.IssuedToken("token-" + userId.uuid() + "-" + accountId.uuid(), 3600);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);
    private final LoginProtectionService protection =
            new LoginProtectionService(new InMemoryFailureCounterRepository(), clock);

    private final SessionIssuer issuer = new SessionIssuer(tokens, new InMemoryRefreshTokenRepository(), clock,
            java.time.Duration.ofDays(30));
    private final InMemoryAuditEventRepository auditRepo = new InMemoryAuditEventRepository();
    private final AuditLog audit = new AuditLog(auditRepo, clock);
    private final RegistrarUsuarioUseCase registrar =
            new RegistrarUsuarioUseCase(users, accounts, encoder, issuer, audit);
    private final AutenticarUseCase autenticar =
            new AutenticarUseCase(users, accounts, encoder, issuer, protection, audit);

    @Test
    void should_auditFailedLogin_withHashedEmailAndReason() {
        registrar.execute("hugo@example.com", "Hugo", "senha-forte-123");

        assertThatThrownBy(() -> autenticar.execute("Hugo@example.com", "errada-123"))
                .isInstanceOf(AuthenticationFailedException.class);

        var falhas = auditRepo.all().stream()
                .filter(e -> e.type() == dio.budgeting.domain.AuditEventType.LOGIN_FALHOU)
                .toList();
        assertThat(falhas).hasSize(1);
        assertThat(falhas.get(0).subjectHash()).isEqualTo(dio.budgeting.domain.AuditEvent.hashSubject("hugo@example.com"));
        assertThat(falhas.get(0).subjectHash()).doesNotContain("hugo");
        assertThat(falhas.get(0).reason()).isEqualTo("CREDENCIAIS_INVALIDAS");
        assertThat(falhas.get(0).actor()).isNotNull();
    }

    @Test
    void should_auditSuccessfulLogin_withoutSecrets() {
        registrar.execute("iris@example.com", "Iris", "senha-forte-123");

        autenticar.execute("iris@example.com", "senha-forte-123");

        assertThat(auditRepo.all()).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo(dio.budgeting.domain.AuditEventType.LOGIN_REALIZADO);
            assertThat(e.outcome()).isEqualTo(dio.budgeting.domain.AuditOutcome.SUCESSO);
            assertThat(e.subjectHash()).isNull();
        });
    }

    @Test
    void should_registerUser_openAccount_andReturnSession() {
        var session = registrar.execute("Ana@Example.com ", "Ana", "senha-forte-123");

        assertThat(session.email()).isEqualTo("ana@example.com");
        assertThat(session.accessToken()).startsWith("token-");
        assertThat(accounts.findByOwner(users.findByEmail("ana@example.com").orElseThrow().id())).isPresent();
    }

    @Test
    void should_neverStorePlainPassword() {
        registrar.execute("bia@example.com", "Bia", "senha-forte-123");

        var stored = users.findByEmail("bia@example.com").orElseThrow();
        assertThat(stored.passwordHash()).isNotEqualTo("senha-forte-123");
        assertThat(encoder.matches("senha-forte-123", stored.passwordHash())).isTrue();
    }

    @Test
    void should_rejectDuplicateEmail_withConflict() {
        registrar.execute("caio@example.com", "Caio", "senha-forte-123");

        assertThatThrownBy(() -> registrar.execute("CAIO@example.com", "Outro", "outra-senha-1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void should_rejectInvalidEmailAndShortPassword() {
        assertThatThrownBy(() -> registrar.execute("sem-arroba", "X", "senha-forte-123"))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> registrar.execute("ok@example.com", "X", "curta"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void should_login_withCorrectPassword() {
        registrar.execute("dani@example.com", "Dani", "senha-forte-123");

        var session = autenticar.execute("dani@example.com", "senha-forte-123");

        assertThat(session.name()).isEqualTo("Dani");
    }

    @Test
    void should_rejectWrongPassword_andUnknownEmail_withSameError() {
        registrar.execute("eli@example.com", "Eli", "senha-forte-123");

        assertThatThrownBy(() -> autenticar.execute("eli@example.com", "errada-123"))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> autenticar.execute("ninguem@example.com", "senha-forte-123"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void should_blockEmail_afterFiveWrongPasswords_evenWithTheCorrectOne() {
        registrar.execute("fer@example.com", "Fer", "senha-forte-123");

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> autenticar.execute("fer@example.com", "errada-123"))
                    .isInstanceOf(AuthenticationFailedException.class);
        }

        assertThatThrownBy(() -> autenticar.execute("fer@example.com", "senha-forte-123"))
                .isInstanceOf(LoginBlockedException.class);
    }

    @Test
    void should_resetEmailCounter_afterSuccessfulLogin() {
        registrar.execute("gui@example.com", "Gui", "senha-forte-123");
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> autenticar.execute("gui@example.com", "errada-123"))
                    .isInstanceOf(AuthenticationFailedException.class);
        }
        autenticar.execute("gui@example.com", "senha-forte-123");

        // Depois do sucesso, o contador volta a zero: são necessárias 5 novas falhas para bloquear.
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> autenticar.execute("gui@example.com", "errada-123"))
                    .isInstanceOf(AuthenticationFailedException.class);
        }
        assertThat(autenticar.execute("gui@example.com", "senha-forte-123").name()).isEqualTo("Gui");
    }
}
