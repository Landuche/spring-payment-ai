package dio.budgeting.auth.application;

import dio.budgeting.account.application.InMemoryAccountRepository;
import dio.budgeting.account.domain.Account;
import dio.budgeting.account.domain.AccountId;
import dio.budgeting.audit.application.AuditLog;
import dio.budgeting.audit.application.InMemoryAuditEventRepository;
import dio.budgeting.auth.domain.AuthenticationFailedException;
import dio.budgeting.auth.domain.RefreshToken;
import dio.budgeting.auth.domain.User;
import dio.budgeting.auth.domain.UserId;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshSessionUseCaseTest {

    private final Instant[] now = {Instant.parse("2026-11-03T12:00:00Z")};
    private final Clock clock = new Clock() {
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now[0]; }
    };

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryRefreshTokenRepository refreshTokens = new InMemoryRefreshTokenRepository();
    private final TokenProvider tokens = (u, a) -> new TokenProvider.IssuedToken("access-" + u.uuid(), 900);
    private final SessionIssuer issuer = new SessionIssuer(tokens, refreshTokens, clock, Duration.ofDays(30));
    private final RefreshSessionUseCase refresh =
            new RefreshSessionUseCase(refreshTokens, users, accounts, issuer,
                    new AuditLog(new InMemoryAuditEventRepository(), clock), clock);

    private final User user = users.save(new User(new UserId(), "ana@example.com", "Ana", "hash"));
    private final Account account = accounts.save(new Account(new AccountId(), user.id(), "0000000001"));

    @Test
    void should_issueRefreshToken_thatCanBeRenewedOnce() {
        var session = issuer.issue(user, account);

        var create = refresh.execute(session.refreshToken());

        assertThat(create.refreshToken()).isNotEqualTo(session.refreshToken());
        assertThat(create.accessToken()).startsWith("access-");
    }

    @Test
    void should_rejectOldRefreshToken_afterRotation() {
        var session = issuer.issue(user, account);
        refresh.execute(session.refreshToken());

        assertThatThrownBy(() -> refresh.execute(session.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void should_revokeWholeFamily_whenUsedTokenIsReplayed() {
        var first = issuer.issue(user, account);
        var second = refresh.execute(first.refreshToken());

        // Replay do token já trocado: sinal de roubo. A sessão inteira (inclusive a nova) é revogada.
        assertThatThrownBy(() -> refresh.execute(first.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> refresh.execute(second.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void should_rejectExpiredToken() {
        var session = issuer.issue(user, account);
        now[0] = now[0].plus(Duration.ofDays(30));

        assertThatThrownBy(() -> refresh.execute(session.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void should_storeOnlyTheHash_notTheToken() {
        var session = issuer.issue(user, account);

        assertThat(refreshTokens.findByHash(session.refreshToken())).isEmpty();
        assertThat(refreshTokens.findByHash(RefreshToken.hash(session.refreshToken()))).isPresent();
    }

    @Test
    void should_rejectUnknownToken() {
        assertThatThrownBy(() -> refresh.execute("nao-existe"))
                .isInstanceOf(AuthenticationFailedException.class);
    }
}
