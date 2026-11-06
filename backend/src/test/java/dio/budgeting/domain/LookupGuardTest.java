package dio.budgeting.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LookupGuardTest {

    private static final Instant AGORA = Instant.parse("2026-11-03T12:00:00Z");

    @Test
    void should_allowTenSearches_andBlockTheEleventh_forOneHour() {
        var guard = LookupGuard.fresh(new AccountId());

        for (int i = 0; i < LookupGuard.MAX_BUSCAS; i++) {
            guard = guard.registrarBusca(AGORA);
            assertThat(guard.isBlocked(AGORA)).isFalse();
        }
        guard = guard.registrarBusca(AGORA);

        assertThat(guard.isBlocked(AGORA)).isTrue();
        assertThat(guard.blockedUntil()).isEqualTo(AGORA.plus(Duration.ofHours(1)));
    }

    @Test
    void should_stayBlocked_untilTheHourPasses() {
        var guard = blockedAt(AGORA);

        assertThat(guard.isBlocked(AGORA.plus(Duration.ofMinutes(59)))).isTrue();
        assertThat(guard.isBlocked(AGORA.plus(Duration.ofHours(1)))).isFalse();
    }

    @Test
    void should_startCountingAgain_afterTheBlockExpires() {
        var guard = blockedAt(AGORA);
        var depois = AGORA.plus(Duration.ofHours(1)).plusSeconds(1);

        guard = guard.registrarBusca(depois);

        assertThat(guard.isBlocked(depois)).isFalse();
        assertThat(guard.attempts()).isEqualTo(1);
    }

    @Test
    void should_resetCounter_whenTransferCompletes() {
        var guard = LookupGuard.fresh(new AccountId());
        for (int i = 0; i < 5; i++) {
            guard = guard.registrarBusca(AGORA);
        }

        assertThat(guard.limpar().attempts()).isZero();
    }

    private static LookupGuard blockedAt(Instant agora) {
        var guard = LookupGuard.fresh(new AccountId());
        for (int i = 0; i <= LookupGuard.MAX_BUSCAS; i++) {
            guard = guard.registrarBusca(agora);
        }
        return guard;
    }
}
