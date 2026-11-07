package dio.budgeting.audit.application;

import dio.budgeting.audit.domain.AuditEvent;
import dio.budgeting.audit.domain.AuditEventRepository;

import java.util.List;

public class InMemoryAuditEventRepository implements AuditEventRepository {
    private final List<AuditEvent> store = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Override
    public void save(AuditEvent event) {
        store.add(event);
    }

    public List<AuditEvent> all() {
        return List.copyOf(store);
    }
}
