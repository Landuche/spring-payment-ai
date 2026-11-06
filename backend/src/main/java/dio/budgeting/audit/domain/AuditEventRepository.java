package dio.budgeting.audit.domain;

/** Trilha append-only: não existe update nem delete. O banco também recusa ambos (trigger na migração). */
public interface AuditEventRepository {
    void save(AuditEvent event);
}
