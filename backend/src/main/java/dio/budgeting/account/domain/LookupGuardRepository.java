package dio.budgeting.account.domain;

import java.time.Instant;
import java.util.Optional;

public interface LookupGuardRepository {
    Optional<LookupGuard> findByAccount(AccountId accountId);

    LookupGuard save(LookupGuard guard);

    /** Remove contadores sem busca acumulada e sem bloqueio ativo: sem eles, a linha equivale a um registro novo. */
    int deleteIdle(Instant now);
}
