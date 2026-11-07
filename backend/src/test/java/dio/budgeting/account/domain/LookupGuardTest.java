package dio.budgeting.account.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LookupGuardTest {

    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");

    @Test
    void should_allowTenSearches_andBlockTheEleventh_forOneHour() {
        var guard = LookupGuard.fresh(new AccountId());

        for (int i = 0; i < LookupGuard.MAX_LOOKUPS; i++) {
            guard = guard.recordLookup(NOW);
            assertThat(guard.isBlocked(NOW)).isFalse();
        }
        guard = guard.recordLookup(NOW);

        assertThat(guard.isBlocked(NOW)).isTrue();
        assertThat(guard.blockedUntil()).isEqualTo(NOW.plus(Duration.ofHours(1)));
    }

    @Test
    void should_stayBlocked_untilTheHourPasses() {
        var guard = blockedAt(NOW);

        assertThat(guard.isBlocked(NOW.plus(Duration.ofMinutes(59)))).isTrue();
        assertThat(guard.isBlocked(NOW.plus(Duration.ofHours(1)))).isFalse();
    }

    @Test
    void should_startCountingAgain_afterTheBlockExpires() {
        var guard = blockedAt(NOW);
        var later = NOW.plus(Duration.ofHours(1)).plusSeconds(1);

        guard = guard.recordLookup(later);

        assertThat(guard.isBlocked(later)).isFalse();
        assertThat(guard.attempts()).isEqualTo(1);
    }

    @Test
    void should_resetCounter_whenTransferCompletes() {
        var guard = LookupGuard.fresh(new AccountId());
        for (int i = 0; i < 5; i++) {
            guard = guard.recordLookup(NOW);
        }

        assertThat(guard.reset().attempts()).isZero();
    }

    private static LookupGuard blockedAt(Instant now) {
        var guard = LookupGuard.fresh(new AccountId());
        for (int i = 0; i <= LookupGuard.MAX_LOOKUPS; i++) {
            guard = guard.recordLookup(now);
        }
        return guard;
    }
}
