package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Account;
import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.AccountRepository;
import dio.budgeting.domain.UserId;
import dio.budgeting.infrastructure.persistence.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

interface AccountEntityRepository extends CrudRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByOwnerId(UUID ownerId);

    Optional<AccountEntity> findByNumber(String number);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.id = :id")
    Optional<AccountEntity> lockById(@Param("id") UUID id);
}

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
