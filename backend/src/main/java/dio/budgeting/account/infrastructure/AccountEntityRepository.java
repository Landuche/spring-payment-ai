package dio.budgeting.account.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface AccountEntityRepository extends CrudRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByOwnerId(UUID ownerId);

    Optional<AccountEntity> findByNumber(String number);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.id = :id")
    Optional<AccountEntity> lockById(@Param("id") UUID id);
}
