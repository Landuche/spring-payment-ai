package dio.budgeting.application;

import dio.budgeting.application.port.TokenProvider;
import dio.budgeting.domain.Account;
import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.AuthenticationFailedException;
import dio.budgeting.domain.RefreshToken;
import dio.budgeting.domain.User;
import dio.budgeting.domain.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenovarSessaoUseCaseTest {

    private final Instant[] agora = {Instant.parse("2026-11-03T12:00:00Z")};
    private final Clock clock = new Clock() {
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora[0]; }
    };

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryRefreshTokenRepository refreshTokens = new InMemoryRefreshTokenRepository();
    private final TokenProvider tokens = (u, a) -> new TokenProvider.IssuedToken("access-" + u.uuid(), 900);
    private final SessionIssuer issuer = new SessionIssuer(tokens, refreshTokens, clock, Duration.ofDays(30));
    private final RenovarSessaoUseCase renovar =
            new RenovarSessaoUseCase(refreshTokens, users, accounts, issuer,
                    new AuditLog(new InMemoryAuditEventRepository(), clock), clock);

    private final User user = users.save(new User(new UserId(), "ana@example.com", "Ana", "hash"));
    private final Account account = accounts.save(new Account(new AccountId(), user.id(), "0000000001"));

    @Test
    void should_issueRefreshToken_thatCanBeRenewedOnce() {
        var sessao = issuer.emitir(user, account);

        var nova = renovar.execute(sessao.refreshToken());

        assertThat(nova.refreshToken()).isNotEqualTo(sessao.refreshToken());
        assertThat(nova.accessToken()).startsWith("access-");
    }

    @Test
    void should_rejectOldRefreshToken_afterRotation() {
        var sessao = issuer.emitir(user, account);
        renovar.execute(sessao.refreshToken());

        assertThatThrownBy(() -> renovar.execute(sessao.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void should_revokeWholeFamily_whenUsedTokenIsReplayed() {
        var primeira = issuer.emitir(user, account);
        var segunda = renovar.execute(primeira.refreshToken());

        // Replay do token já trocado: sinal de roubo. A sessão inteira (inclusive a nova) é revogada.
        assertThatThrownBy(() -> renovar.execute(primeira.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> renovar.execute(segunda.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void should_rejectExpiredToken() {
        var sessao = issuer.emitir(user, account);
        agora[0] = agora[0].plus(Duration.ofDays(30));

        assertThatThrownBy(() -> renovar.execute(sessao.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void should_storeOnlyTheHash_notTheToken() {
        var sessao = issuer.emitir(user, account);

        assertThat(refreshTokens.findByHash(sessao.refreshToken())).isEmpty();
        assertThat(refreshTokens.findByHash(RefreshToken.hash(sessao.refreshToken()))).isPresent();
    }

    @Test
    void should_rejectUnknownToken() {
        assertThatThrownBy(() -> renovar.execute("nao-existe"))
                .isInstanceOf(AuthenticationFailedException.class);
    }
}
