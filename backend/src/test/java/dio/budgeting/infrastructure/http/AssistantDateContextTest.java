package dio.budgeting.infrastructure.http;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantDateContextTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Test
    void should_informTodayAndCurrentMonth_inPortuguese() {
        var clock = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), SAO_PAULO);

        var contexto = AssistantController.contextoDeData(clock);

        assertThat(contexto)
                .contains("quarta-feira, 04/11/2026")
                .contains("novembro de 2026")
                .contains("2026-11");
    }

    @Test
    void should_useTheClockZone_whenUtcIsAlreadyTomorrow() {
        // 01:30 UTC de 05/11 ainda é 22:30 de 04/11 em São Paulo.
        var clock = Clock.fixed(Instant.parse("2026-11-05T01:30:00Z"), SAO_PAULO);

        assertThat(AssistantController.contextoDeData(clock)).contains("04/11/2026");
    }
}
