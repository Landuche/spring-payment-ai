package dio.budgeting.outbox.application;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Guarda os eventos pedidos, sem serializar nem gravar em banco. */
public class InMemoryOutboxRecorder implements OutboxRecorder {

    public record Recorded(String topic, UUID aggregateId, String eventType, Object payload) {
    }

    private final List<Recorded> recorded = new ArrayList<>();

    @Override
    public void record(String topic, UUID aggregateId, String eventType, Object payload) {
        recorded.add(new Recorded(topic, aggregateId, eventType, payload));
    }

    public List<Recorded> all() {
        return List.copyOf(recorded);
    }
}
