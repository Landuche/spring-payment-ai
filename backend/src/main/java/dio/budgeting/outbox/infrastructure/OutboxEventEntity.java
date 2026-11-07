package dio.budgeting.outbox.infrastructure;

import dio.budgeting.outbox.domain.OutboxEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class OutboxEventEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    static OutboxEventEntity from(OutboxEvent event) {
        return new OutboxEventEntity(event.id(), event.topic(), event.aggregateId(), event.eventType(),
                event.payload(), event.createdAt(), event.publishedAt());
    }

    OutboxEvent toDomain() {
        return new OutboxEvent(id, topic, aggregateId, eventType, payload, createdAt, publishedAt);
    }
}
