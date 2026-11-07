package dio.budgeting.audit.infrastructure;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

interface AuditEventEntityRepository extends CrudRepository<AuditEventEntity, UUID> {
}
