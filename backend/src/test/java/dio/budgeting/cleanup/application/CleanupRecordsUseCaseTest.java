package dio.budgeting.cleanup.application;

import dio.budgeting.cleanup.domain.CleanupRepository;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CleanupRecordsUseCaseTest {

    private final Instant now = Instant.parse("2026-11-03T06:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @Test
    void should_passOneDayCutoffToFailureCounters_andNowToTheOthers() {
        var repo = new RecordingCleanup(3, 5, 7);
        var useCase = new CleanupRecordsUseCase(repo, clock);

        var result = useCase.execute();

        assertThat(repo.countersCutoff).isEqualTo(now.minusSeconds(24 * 3600));
        assertThat(repo.nowLookups).isEqualTo(now);
        assertThat(repo.nowTokens).isEqualTo(now);
        assertThat(result).isEqualTo(new CleanupRecordsUseCase.Result(3, 5, 7));
    }

    private static final class RecordingCleanup implements CleanupRepository {
        private final int counters;
        private final int lookups;
        private final int tokens;
        Instant countersCutoff;
        Instant nowLookups;
        Instant nowTokens;

        RecordingCleanup(int counters, int lookups, int tokens) {
            this.counters = counters;
            this.lookups = lookups;
            this.tokens = tokens;
        }

        @Override
        public int deleteInactiveCounters(Instant cutoff) {
            countersCutoff = cutoff;
            return counters;
        }

        @Override
        public int deleteIdleLookups(Instant now) {
            nowLookups = now;
            return lookups;
        }

        @Override
        public int deleteExpiredRefreshTokens(Instant now) {
            nowTokens = now;
            return tokens;
        }
    }
}
