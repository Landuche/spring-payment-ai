package dio.budgeting.outbox.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de domínio à espera de publicação. É gravado na mesma transação do fato de negócio que o originou,
 * então os dois existem ou não existem juntos; um publicador o envia ao broker depois.
 */
public record OutboxEvent(
        UUID id,
        String topic,
        UUID aggregateId,
        String eventType,
        String payload,
        Instant createdAt,
        Instant publishedAt) {

    public OutboxEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static OutboxEvent pending(String topic, UUID aggregateId, String eventType, String payload, Instant now) {
        return new OutboxEvent(UUID.randomUUID(), topic, aggregateId, eventType, payload, now, null);
    }

    public boolean isPublished() {
        return publishedAt != null;
    }

    public OutboxEvent markPublished(Instant at) {
        return new OutboxEvent(id, topic, aggregateId, eventType, payload, createdAt, at);
    }
}
