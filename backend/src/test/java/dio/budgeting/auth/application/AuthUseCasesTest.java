package dio.budgeting.auth.application;

import dio.budgeting.account.application.InMemoryAccountRepository;
import dio.budgeting.audit.application.AuditLog;
import dio.budgeting.audit.application.InMemoryAuditEventRepository;
import dio.budgeting.auth.domain.AuthenticationFailedException;
import dio.budgeting.auth.domain.LoginBlockedException;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.ConflictException;

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
    private final RegisterUserUseCase register =
            new RegisterUserUseCase(users, accounts, encoder, issuer, audit);
    private final LoginUseCase login =
            new LoginUseCase(users, accounts, encoder, issuer, protection, audit);

    @Test
    void should_auditFailedLogin_withHashedEmailAndReason() {
        register.execute("hugo@example.com", "Hugo", "senha-forte-123");

        assertThatThrownBy(() -> login.execute("Hugo@example.com", "errada-123"))
                .isInstanceOf(AuthenticationFailedException.class);

        var failures = auditRepo.all().stream()
                .filter(e -> e.type() == dio.budgeting.audit.domain.AuditEventType.LOGIN_FAILED)
                .toList();
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).subjectHash()).isEqualTo(dio.budgeting.audit.domain.AuditEvent.hashSubject("hugo@example.com"));
        assertThat(failures.get(0).subjectHash()).doesNotContain("hugo");
        assertThat(failures.get(0).reason()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(failures.get(0).actor()).isNotNull();
    }

    @Test
    void should_auditSuccessfulLogin_withoutSecrets() {
        register.execute("iris@example.com", "Iris", "senha-forte-123");

        login.execute("iris@example.com", "senha-forte-123");

        assertThat(auditRepo.all()).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo(dio.budgeting.audit.domain.AuditEventType.LOGIN_SUCCEEDED);
            assertThat(e.outcome()).isEqualTo(dio.budgeting.audit.domain.AuditOutcome.SUCCESS);
            assertThat(e.subjectHash()).isNull();
        });
    }

    @Test
    void should_registerUser_openAccount_andReturnSession() {
        var session = register.execute("Ana@Example.com ", "Ana", "senha-forte-123");

        assertThat(session.email()).isEqualTo("ana@example.com");
        assertThat(session.accessToken()).startsWith("token-");
        assertThat(accounts.findByOwner(users.findByEmail("ana@example.com").orElseThrow().id())).isPresent();
    }

    @Test
    void should_neverStorePlainPassword() {
        register.execute("bia@example.com", "Bia", "senha-forte-123");

        var stored = users.findByEmail("bia@example.com").orElseThrow();
        assertThat(stored.passwordHash()).isNotEqualTo("senha-forte-123");
        assertThat(encoder.matches("senha-forte-123", stored.passwordHash())).isTrue();
    }

    @Test
    void should_rejectDuplicateEmail_withConflict() {
        register.execute("caio@example.com", "Caio", "senha-forte-123");

        assertThatThrownBy(() -> register.execute("CAIO@example.com", "Outro", "outra-senha-1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void should_rejectInvalidEmailAndShortPassword() {
        assertThatThrownBy(() -> register.execute("sem-arroba", "X", "senha-forte-123"))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> register.execute("ok@example.com", "X", "curta"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void should_login_withCorrectPassword() {
        register.execute("dani@example.com", "Dani", "senha-forte-123");

        var session = login.execute("dani@example.com", "senha-forte-123");

        assertThat(session.name()).isEqualTo("Dani");
    }

    @Test
    void should_rejectWrongPassword_andUnknownEmail_withSameError() {
        register.execute("eli@example.com", "Eli", "senha-forte-123");

        assertThatThrownBy(() -> login.execute("eli@example.com", "errada-123"))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> login.execute("ninguem@example.com", "senha-forte-123"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void should_blockEmail_afterFiveWrongPasswords_evenWithTheCorrectOne() {
        register.execute("fer@example.com", "Fer", "senha-forte-123");

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> login.execute("fer@example.com", "errada-123"))
                    .isInstanceOf(AuthenticationFailedException.class);
        }

        assertThatThrownBy(() -> login.execute("fer@example.com", "senha-forte-123"))
                .isInstanceOf(LoginBlockedException.class);
    }

    @Test
    void should_resetEmailCounter_afterSuccessfulLogin() {
        register.execute("gui@example.com", "Gui", "senha-forte-123");
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> login.execute("gui@example.com", "errada-123"))
                    .isInstanceOf(AuthenticationFailedException.class);
        }
        login.execute("gui@example.com", "senha-forte-123");

        // Depois do sucesso, o contador volta a zero: são necessárias 5 novas falhas para bloquear.
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> login.execute("gui@example.com", "errada-123"))
                    .isInstanceOf(AuthenticationFailedException.class);
        }
        assertThat(login.execute("gui@example.com", "senha-forte-123").name()).isEqualTo("Gui");
    }
}
