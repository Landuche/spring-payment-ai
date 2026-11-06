package dio.budgeting.cleanup.application;

import dio.budgeting.cleanup.domain.LimpezaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Manutenção periódica do banco. Cada regra só apaga o que já não tem efeito sobre segurança:
 * <ul>
 *   <li>contadores de falha com mais de 1 dia, sem bloqueio ativo (a janela de login é de 15 min);</li>
 *   <li>contadores de busca zerados e sem bloqueio ativo (equivalem a um registro novo);</li>
 *   <li>refresh tokens expirados.</li>
 * </ul>
 * A margem de 1 dia nos contadores é conservadora de propósito: apaga um pouco menos do que o mínimo necessário.
 */
@Service
public class LimpezaRegistrosUseCase {
    private static final Logger log = LoggerFactory.getLogger(LimpezaRegistrosUseCase.class);
    static final Duration MARGEM_CONTADORES = Duration.ofDays(1);

    private final LimpezaRepository limpeza;
    private final Clock clock;

    public LimpezaRegistrosUseCase(LimpezaRepository limpeza, Clock clock) {
        this.limpeza = limpeza;
        this.clock = clock;
    }

    @Transactional
    public Resultado executar() {
        var agora = Instant.now(clock);
        var resultado = new Resultado(
                limpeza.apagarContadoresInativos(agora.minus(MARGEM_CONTADORES)),
                limpeza.apagarBuscasSemPendencia(agora),
                limpeza.apagarRefreshTokensExpirados(agora));
        log.info("Limpeza concluída: contadores de login={}, buscas={}, refresh tokens={}",
                resultado.contadoresLogin(), resultado.buscas(), resultado.refreshTokens());
        return resultado;
    }

    public record Resultado(int contadoresLogin, int buscas, int refreshTokens) {
    }
}
