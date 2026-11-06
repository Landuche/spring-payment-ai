package dio.budgeting.account.domain;

import dio.budgeting.auth.domain.UserId;

import java.util.Optional;

public interface AccountRepository {
    Account save(Account account);

    Optional<Account> findById(AccountId id);

    Optional<Account> findByOwner(UserId ownerId);

    Optional<Account> findByNumber(String number);

    /** Trava a linha da conta até o fim da transação atual, serializando operações concorrentes. */
    void lockForUpdate(AccountId id);
}
