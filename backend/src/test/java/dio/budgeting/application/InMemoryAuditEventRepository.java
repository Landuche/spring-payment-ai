package dio.budgeting.application;

import dio.budgeting.domain.AuditEvent;
import dio.budgeting.domain.AuditEventRepository;

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
