package dio.budgeting.account.infrastructure;

import dio.budgeting.account.domain.Account;
import dio.budgeting.account.domain.AccountId;
import dio.budgeting.account.domain.AccountRepository;
import dio.budgeting.auth.domain.UserId;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class AccountJpaRepository implements AccountRepository {
    private final AccountEntityRepository entityRepository;

    AccountJpaRepository(AccountEntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Override
    public Account save(Account account) {
        return entityRepository.save(AccountEntity.from(account)).toDomain();
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return entityRepository.findById(id.uuid()).map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findByOwner(UserId ownerId) {
        return entityRepository.findByOwnerId(ownerId.uuid()).map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findByNumber(String number) {
        return entityRepository.findByNumber(number).map(AccountEntity::toDomain);
    }

    @Override
    public void lockForUpdate(AccountId id) {
        entityRepository.lockById(id.uuid())
                .orElseThrow(() -> new IllegalStateException("Conta inexistente: " + id));
    }
}
