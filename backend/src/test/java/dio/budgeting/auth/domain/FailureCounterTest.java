package dio.budgeting.auth.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FailureCounterTest {

    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    @Test
    void should_blockOnTheFifthFailure_withinTheWindow() {
        var c = FailureCounter.fresh("email:a@x.com");
        for (int i = 0; i < 4; i++) {
            c = c.recordFailure(NOW, 5, WINDOW, BLOCK_DURATION);
            assertThat(c.isBlocked(NOW)).isFalse();
        }
        c = c.recordFailure(NOW, 5, WINDOW, BLOCK_DURATION);

        assertThat(c.isBlocked(NOW)).isTrue();
        assertThat(c.blockedUntil()).isEqualTo(NOW.plus(BLOCK_DURATION));
    }

    @Test
    void should_notAccumulate_failuresAcrossWindows() {
        var c = FailureCounter.fresh("email:a@x.com");
        for (int i = 0; i < 4; i++) {
            c = c.recordFailure(NOW, 5, WINDOW, BLOCK_DURATION);
        }

        // Depois da janela, as 4 falhas antigas expiram: a próxima é a primeira da nova janela.
        var later = NOW.plus(WINDOW).plusSeconds(1);
        c = c.recordFailure(later, 5, WINDOW, BLOCK_DURATION);

        assertThat(c.isBlocked(later)).isFalse();
        assertThat(c.failures()).isEqualTo(1);
    }

    @Test
    void should_unblock_afterTheBlockPeriod() {
        var c = FailureCounter.fresh("ip:10.0.0.1");
        for (int i = 0; i < 5; i++) {
            c = c.recordFailure(NOW, 5, WINDOW, BLOCK_DURATION);
        }

        assertThat(c.isBlocked(NOW.plus(BLOCK_DURATION).minusSeconds(1))).isTrue();
        assertThat(c.isBlocked(NOW.plus(BLOCK_DURATION))).isFalse();
    }

    @Test
    void should_clearOnSuccessfulLogin() {
        var c = FailureCounter.fresh("email:a@x.com").recordFailure(NOW, 5, WINDOW, BLOCK_DURATION);

        assertThat(c.reset().failures()).isZero();
        assertThat(c.reset().isBlocked(NOW)).isFalse();
    }
}
