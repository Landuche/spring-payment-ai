package dio.budgeting.transfer.infrastructure;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

interface TransferEntityRepository extends CrudRepository<TransferEntity, UUID> {
    Optional<TransferEntity> findByOriginAccountIdAndIdempotencyKey(UUID originAccountId, String idempotencyKey);
}
