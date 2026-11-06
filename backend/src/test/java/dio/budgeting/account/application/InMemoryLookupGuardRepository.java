package dio.budgeting.account.application;


import dio.budgeting.account.domain.AccountId;
import dio.budgeting.account.domain.LookupGuard;
import dio.budgeting.account.domain.LookupGuardRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryLookupGuardRepository implements LookupGuardRepository {
    private final Map<AccountId, LookupGuard> store = new HashMap<>();

    @Override
    public Optional<LookupGuard> findByAccount(AccountId accountId) {
        return Optional.ofNullable(store.get(accountId));
    }

    @Override
    public LookupGuard save(LookupGuard guard) {
        store.put(guard.accountId(), guard);
        return guard;
    }
}
