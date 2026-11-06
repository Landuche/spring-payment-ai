package dio.budgeting.account.domain;

import java.util.Optional;

public interface LookupGuardRepository {
    Optional<LookupGuard> findByAccount(AccountId accountId);

    LookupGuard save(LookupGuard guard);
}
