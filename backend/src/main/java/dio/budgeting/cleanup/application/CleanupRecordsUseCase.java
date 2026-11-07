package dio.budgeting.cleanup.application;

import dio.budgeting.cleanup.domain.CleanupRepository;

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
public class CleanupRecordsUseCase {
    private static final Logger log = LoggerFactory.getLogger(CleanupRecordsUseCase.class);
    static final Duration COUNTER_MARGIN = Duration.ofDays(1);

    private final CleanupRepository cleanup;
    private final Clock clock;

    public CleanupRecordsUseCase(CleanupRepository cleanup, Clock clock) {
        this.cleanup = cleanup;
        this.clock = clock;
    }

    @Transactional
    public Result execute() {
        var now = Instant.now(clock);
        var result = new Result(
                cleanup.deleteInactiveCounters(now.minus(COUNTER_MARGIN)),
                cleanup.deleteIdleLookups(now),
                cleanup.deleteExpiredRefreshTokens(now));
        log.info("Limpeza concluída: contadores de login={}, buscas={}, refresh tokens={}",
                result.loginCounters(), result.lookups(), result.refreshTokens());
        return result;
    }

    public record Result(int loginCounters, int lookups, int refreshTokens) {
    }
}
