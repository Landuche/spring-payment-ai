package dio.budgeting.account.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Proteção contra enumeração de contas. Cada busca de destinatário (por número ou e-mail) conta;
 * ao ultrapassar {@link #MAX_LOOKUPS} sem uma transferência concluída, a conta fica bloqueada por {@link #BLOCK_DURATION}.
 */
public record LookupGuard(AccountId accountId, int attempts, Instant blockedUntil) {

    public static final int MAX_LOOKUPS = 10;
    public static final Duration BLOCK_DURATION = Duration.ofHours(1);

    public static LookupGuard fresh(AccountId accountId) {
        return new LookupGuard(accountId, 0, null);
    }

    public boolean isBlocked(Instant now) {
        return blockedUntil != null && now.isBefore(blockedUntil);
    }

    /** Registra uma busca. Se o limite for ultrapassado, bloqueia. */
    public LookupGuard recordLookup(Instant now) {
        if (isBlocked(now)) {
            return this;
        }
        int next = attempts + 1;
        if (next > MAX_LOOKUPS) {
            return new LookupGuard(accountId, 0, now.plus(BLOCK_DURATION));
        }
        return new LookupGuard(accountId, next, null);
    }

    /** Zera o contador depois de uma transferência concluída. */
    public LookupGuard reset() {
        return fresh(accountId);
    }
}
