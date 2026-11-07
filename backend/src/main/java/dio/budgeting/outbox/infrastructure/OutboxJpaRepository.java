package dio.budgeting.outbox.infrastructure;

import dio.budgeting.outbox.domain.OutboxEvent;
import dio.budgeting.outbox.domain.OutboxRepository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class OutboxJpaRepository implements OutboxRepository {
    private final OutboxEventEntityRepository entityRepository;

    OutboxJpaRepository(OutboxEventEntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return entityRepository.save(OutboxEventEntity.from(event)).toDomain();
    }

    @Override
    public List<OutboxEvent> findPendingForUpdate(int limit) {
        return entityRepository.findPendingForUpdate(limit).stream().map(OutboxEventEntity::toDomain).toList();
    }
}
