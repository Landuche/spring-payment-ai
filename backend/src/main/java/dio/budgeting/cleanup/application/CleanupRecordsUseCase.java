package dio.budgeting.cleanup.application;

import dio.budgeting.account.domain.LookupGuardRepository;
import dio.budgeting.auth.domain.FailureCounterRepository;
import dio.budgeting.auth.domain.RefreshTokenRepository;
import dio.budgeting.outbox.domain.OutboxRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Manutenção periódica do banco.
 * <ul>
 *   <li>contadores de falha com mais de 1 dia, sem bloqueio ativo (a janela de login é de 15 min)</li>
 *   <li>contadores de busca zerados e sem bloqueio ativo</li>
 *   <li>refresh tokens expirados</li>
 *   <li>eventos do outbox publicados há mais de {@link #OUTBOX_RETENTION}.</li>
 * </ul>
 */
@Service
public class CleanupRecordsUseCase {
    private static final Logger log = LoggerFactory.getLogger(CleanupRecordsUseCase.class);
    static final Duration COUNTER_MARGIN = Duration.ofDays(1);
    static final Duration OUTBOX_RETENTION = Duration.ofDays(7);

    private final FailureCounterRepository failureCounters;
    private final LookupGuardRepository lookupGuards;
    private final RefreshTokenRepository refreshTokens;
    private final OutboxRepository outbox;
    private final Clock clock;

    public CleanupRecordsUseCase(FailureCounterRepository failureCounters, LookupGuardRepository lookupGuards,
                                 RefreshTokenRepository refreshTokens, OutboxRepository outbox, Clock clock) {
        this.failureCounters = failureCounters;
        this.lookupGuards = lookupGuards;
        this.refreshTokens = refreshTokens;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public Result execute() {
        var now = Instant.now(clock);
        var result = new Result(
                failureCounters.deleteInactive(now.minus(COUNTER_MARGIN)),
                lookupGuards.deleteIdle(now),
                refreshTokens.deleteExpired(now),
                outbox.deletePublishedBefore(now.minus(OUTBOX_RETENTION)));
        log.info("Limpeza concluída: contadores de login={}, buscas={}, refresh tokens={}, eventos publicados={}",
                result.loginCounters(), result.lookups(), result.refreshTokens(), result.outboxEvents());
        return result;
    }

    public record Result(int loginCounters, int lookups, int refreshTokens, int outboxEvents) {
    }
}
