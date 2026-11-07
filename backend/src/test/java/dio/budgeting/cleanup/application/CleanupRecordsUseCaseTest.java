package dio.budgeting.cleanup.application;

import dio.budgeting.account.application.InMemoryLookupGuardRepository;
import dio.budgeting.account.domain.AccountId;
import dio.budgeting.account.domain.LookupGuard;
import dio.budgeting.auth.application.InMemoryFailureCounterRepository;
import dio.budgeting.auth.application.InMemoryRefreshTokenRepository;
import dio.budgeting.auth.domain.FailureCounter;
import dio.budgeting.auth.domain.RefreshToken;
import dio.budgeting.auth.domain.UserId;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CleanupRecordsUseCaseTest {

    private final Instant now = Instant.parse("2026-11-03T06:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final InMemoryFailureCounterRepository failureCounters = new InMemoryFailureCounterRepository();
    private final InMemoryLookupGuardRepository lookupGuards = new InMemoryLookupGuardRepository();
    private final InMemoryRefreshTokenRepository refreshTokens = new InMemoryRefreshTokenRepository();
    private final CleanupRecordsUseCase useCase =
            new CleanupRecordsUseCase(failureCounters, lookupGuards, refreshTokens, clock);

    @Test
    void should_removeOnlyCountersOlderThanTheOneDayMargin_andWithoutActiveBlock() {
        var old = now.minus(Duration.ofDays(2));
        var recent = now.minus(Duration.ofHours(1));
        failureCounters.save(new FailureCounter("old", 3, old, null));
        failureCounters.save(new FailureCounter("recent", 3, recent, null));
        failureCounters.save(new FailureCounter("blocked", 5, old, now.plus(Duration.ofMinutes(5))));

        var result = useCase.execute();

        assertThat(result.loginCounters()).isEqualTo(1);
        assertThat(failureCounters.findByKey("old")).isEmpty();
        assertThat(failureCounters.findByKey("recent")).isPresent();
        assertThat(failureCounters.findByKey("blocked")).isPresent();
    }

    @Test
    void should_removeIdleLookupCounters_keepingTheOnesWithAttemptsOrActiveBlock() {
        var idle = new AccountId();
        var withAttempts = new AccountId();
        var blocked = new AccountId();
        lookupGuards.save(new LookupGuard(idle, 0, null));
        lookupGuards.save(new LookupGuard(withAttempts, 3, null));
        lookupGuards.save(new LookupGuard(blocked, 0, now.plus(Duration.ofMinutes(30))));

        var result = useCase.execute();

        assertThat(result.lookups()).isEqualTo(1);
        assertThat(lookupGuards.findByAccount(idle)).isEmpty();
        assertThat(lookupGuards.findByAccount(withAttempts)).isPresent();
        assertThat(lookupGuards.findByAccount(blocked)).isPresent();
    }

    @Test
    void should_removeOnlyExpiredRefreshTokens() {
        var user = new UserId();
        var family = UUID.randomUUID();
        refreshTokens.save(new RefreshToken("expired", user, family, now.minusSeconds(1), null, null));
        refreshTokens.save(new RefreshToken("valid", user, family, now.plusSeconds(60), null, null));

        var result = useCase.execute();

        assertThat(result.refreshTokens()).isEqualTo(1);
        assertThat(refreshTokens.findByHash("expired")).isEmpty();
        assertThat(refreshTokens.findByHash("valid")).isPresent();
    }
}
