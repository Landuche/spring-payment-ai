package dio.budgeting.outbox.application;

import dio.budgeting.outbox.domain.OutboxEvent;
import dio.budgeting.outbox.domain.OutboxRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InMemoryOutboxRepository implements OutboxRepository {
    private final Map<UUID, OutboxEvent> store = new LinkedHashMap<>();

    @Override
    public OutboxEvent save(OutboxEvent event) {
        store.put(event.id(), event);
        return event;
    }

    @Override
    public List<OutboxEvent> findPendingForUpdate(int limit) {
        return store.values().stream()
                .filter(event -> !event.isPublished())
                .sorted(Comparator.comparing(OutboxEvent::createdAt))
                .limit(limit)
                .toList();
    }

    @Override
    public int deletePublishedBefore(Instant limit) {
        int before = store.size();
        store.values().removeIf(event -> event.isPublished() && event.publishedAt().isBefore(limit));
        return before - store.size();
    }

    public OutboxEvent get(UUID id) {
        return store.get(id);
    }
}
