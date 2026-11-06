package dio.budgeting.audit.infrastructure;

import dio.budgeting.audit.domain.AuditEvent;
import dio.budgeting.audit.domain.AuditEventRepository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

interface AuditEventEntityRepository extends CrudRepository<AuditEventEntity, UUID> {
}

/** Só inserção: a trilha é append-only, e o banco recusa update e delete (trigger na migração). */
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
