package dio.budgeting.application;

import dio.budgeting.domain.LimpezaRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LimpezaRegistrosUseCaseTest {

    private final Instant agora = Instant.parse("2026-11-03T06:00:00Z");
    private final Clock clock = Clock.fixed(agora, ZoneOffset.UTC);

    @Test
    void should_passOneDayCutoffToFailureCounters_andNowToTheOthers() {
        var repo = new GravandoLimpeza(3, 5, 7);
        var useCase = new LimpezaRegistrosUseCase(repo, clock);

        var resultado = useCase.executar();

        assertThat(repo.corteContadores).isEqualTo(agora.minusSeconds(24 * 3600));
        assertThat(repo.agoraBuscas).isEqualTo(agora);
        assertThat(repo.agoraTokens).isEqualTo(agora);
        assertThat(resultado).isEqualTo(new LimpezaRegistrosUseCase.Resultado(3, 5, 7));
    }

    private static final class GravandoLimpeza implements LimpezaRepository {
        private final int contadores;
        private final int buscas;
        private final int tokens;
        Instant corteContadores;
        Instant agoraBuscas;
        Instant agoraTokens;

        GravandoLimpeza(int contadores, int buscas, int tokens) {
            this.contadores = contadores;
            this.buscas = buscas;
            this.tokens = tokens;
        }

        @Override
        public int apagarContadoresInativos(Instant corte) {
            corteContadores = corte;
            return contadores;
        }

        @Override
        public int apagarBuscasSemPendencia(Instant agora) {
            agoraBuscas = agora;
            return buscas;
        }

        @Override
        public int apagarRefreshTokensExpirados(Instant agora) {
            agoraTokens = agora;
            return tokens;
        }
    }
}
