package dio.budgeting.audit.domain;

/** append-only, sem update nem delete. */
public interface AuditEventRepository {
    void save(AuditEvent event);
}
