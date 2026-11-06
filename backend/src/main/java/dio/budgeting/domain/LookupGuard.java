package dio.budgeting.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Proteção contra enumeração de contas. Cada busca de destinatário (por número ou e-mail) conta;
 * ao ultrapassar {@link #MAX_BUSCAS} sem uma transferência concluída, a conta fica bloqueada por {@link #BLOQUEIO}.
 * Imutável: cada operação devolve o novo estado, que o chamador persiste.
 */
public record LookupGuard(AccountId accountId, int attempts, Instant blockedUntil) {

    public static final int MAX_BUSCAS = 10;
    public static final Duration BLOQUEIO = Duration.ofHours(1);

    public static LookupGuard fresh(AccountId accountId) {
        return new LookupGuard(accountId, 0, null);
    }

    public boolean isBlocked(Instant now) {
        return blockedUntil != null && now.isBefore(blockedUntil);
    }

    /** Registra uma busca. Se o limite for ultrapassado, devolve o estado bloqueado. */
    public LookupGuard registrarBusca(Instant now) {
        if (isBlocked(now)) {
            return this;
        }
        int next = attempts + 1;
        if (next > MAX_BUSCAS) {
            return new LookupGuard(accountId, 0, now.plus(BLOQUEIO));
        }
        return new LookupGuard(accountId, next, null);
    }

    /** Zera o contador depois de uma transferência concluída. */
    public LookupGuard limpar() {
        return fresh(accountId);
    }
}
