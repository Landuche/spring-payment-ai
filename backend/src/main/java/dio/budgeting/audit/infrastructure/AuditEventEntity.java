package dio.budgeting.audit.infrastructure;

import dio.budgeting.audit.domain.AuditEvent;
import dio.budgeting.audit.domain.AuditEventType;
import dio.budgeting.audit.domain.AuditOutcome;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AuditEventEntity {
    @Id
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuditOutcome outcome;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "subject_hash", length = 64)
    private String subjectHash;

    @Column(length = 64)
    private String reference;

    @Column(length = 60)
    private String reason;

    public static AuditEventEntity from(AuditEvent event) {
        return new AuditEventEntity(
                event.id(),
                event.occurredAt(),
                event.type(),
                event.outcome(),
                event.actor() == null ? null : event.actor().uuid(),
                event.account() == null ? null : event.account().uuid(),
                event.subjectHash(),
                event.reference(),
                event.reason());
    }
}
