package dio.budgeting.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FailureCounterTest {

    private static final Instant AGORA = Instant.parse("2026-11-03T12:00:00Z");
    private static final Duration JANELA = Duration.ofMinutes(15);
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    @Test
    void should_blockOnTheFifthFailure_withinTheWindow() {
        var c = FailureCounter.fresh("email:a@x.com");
        for (int i = 0; i < 4; i++) {
            c = c.registrarFalha(AGORA, 5, JANELA, BLOQUEIO);
            assertThat(c.isBlocked(AGORA)).isFalse();
        }
        c = c.registrarFalha(AGORA, 5, JANELA, BLOQUEIO);

        assertThat(c.isBlocked(AGORA)).isTrue();
        assertThat(c.blockedUntil()).isEqualTo(AGORA.plus(BLOQUEIO));
    }

    @Test
    void should_notAccumulate_failuresAcrossWindows() {
        var c = FailureCounter.fresh("email:a@x.com");
        for (int i = 0; i < 4; i++) {
            c = c.registrarFalha(AGORA, 5, JANELA, BLOQUEIO);
        }

        // Depois da janela, as 4 falhas antigas expiram: a próxima é a primeira da nova janela.
        var depois = AGORA.plus(JANELA).plusSeconds(1);
        c = c.registrarFalha(depois, 5, JANELA, BLOQUEIO);

        assertThat(c.isBlocked(depois)).isFalse();
        assertThat(c.failures()).isEqualTo(1);
    }

    @Test
    void should_unblock_afterTheBlockPeriod() {
        var c = FailureCounter.fresh("ip:10.0.0.1");
        for (int i = 0; i < 5; i++) {
            c = c.registrarFalha(AGORA, 5, JANELA, BLOQUEIO);
        }

        assertThat(c.isBlocked(AGORA.plus(BLOQUEIO).minusSeconds(1))).isTrue();
        assertThat(c.isBlocked(AGORA.plus(BLOQUEIO))).isFalse();
    }

    @Test
    void should_clearOnSuccessfulLogin() {
        var c = FailureCounter.fresh("email:a@x.com").registrarFalha(AGORA, 5, JANELA, BLOQUEIO);

        assertThat(c.limpar().failures()).isZero();
        assertThat(c.limpar().isBlocked(AGORA)).isFalse();
    }
}
