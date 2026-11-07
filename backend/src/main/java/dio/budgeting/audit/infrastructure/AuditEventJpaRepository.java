package dio.budgeting.audit.infrastructure;

import dio.budgeting.audit.domain.AuditEvent;
import dio.budgeting.audit.domain.AuditEventRepository;

import org.springframework.stereotype.Repository;

/** append-only, e o banco recusa update e delete. */
@Repository
class AuditEventJpaRepository implements AuditEventRepository {
    private final AuditEventEntityRepository entityRepository;

    AuditEventJpaRepository(AuditEventEntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Override
    public void save(AuditEvent event) {
        entityRepository.save(AuditEventEntity.from(event));
    }
}
