package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.Transfer;
import dio.budgeting.domain.TransferRepository;
import dio.budgeting.infrastructure.persistence.entity.TransferEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

interface TransferEntityRepository extends CrudRepository<TransferEntity, UUID> {
    Optional<TransferEntity> findByOriginAccountIdAndIdempotencyKey(UUID originAccountId, String idempotencyKey);
}

@Repository
class TransferJpaRepository implements TransferRepository {
    private final TransferEntityRepository entityRepository;

    TransferJpaRepository(TransferEntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Override
    public Transfer save(Transfer transfer) {
        return entityRepository.save(TransferEntity.from(transfer)).toDomain();
    }

    @Override
    public Optional<Transfer> findByIdempotencyKey(AccountId origin, String idempotencyKey) {
        return entityRepository.findByOriginAccountIdAndIdempotencyKey(origin.uuid(), idempotencyKey)
                .map(TransferEntity::toDomain);
    }
}
