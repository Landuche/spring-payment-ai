package dio.budgeting.application;

import dio.budgeting.domain.Account;
import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.AccountRepository;
import dio.budgeting.domain.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryAccountRepository implements AccountRepository {
    private final List<Account> store = new ArrayList<>();
    private final List<AccountId> locked = new ArrayList<>();

    @Override
    public Account save(Account account) {
        store.add(account);
        return account;
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return store.stream().filter(a -> a.id().equals(id)).findFirst();
    }

    @Override
    public Optional<Account> findByOwner(UserId ownerId) {
        return store.stream().filter(a -> a.ownerId().equals(ownerId)).findFirst();
    }

    @Override
    public Optional<Account> findByNumber(String number) {
        return store.stream().filter(a -> a.number().equals(number)).findFirst();
    }

    @Override
    public void lockForUpdate(AccountId id) {
        locked.add(id);
    }

    public int locks() {
        return locked.size();
    }

    public List<AccountId> lockOrder() {
        return List.copyOf(locked);
    }
}
