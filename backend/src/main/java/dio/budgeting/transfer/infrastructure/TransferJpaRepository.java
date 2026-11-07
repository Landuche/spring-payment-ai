package dio.budgeting.transfer.infrastructure;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.transfer.domain.Transfer;
import dio.budgeting.transfer.domain.TransferRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

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
