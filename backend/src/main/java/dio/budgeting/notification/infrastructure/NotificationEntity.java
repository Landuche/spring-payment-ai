package dio.budgeting.notification.infrastructure;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.notification.domain.Notification;
import dio.budgeting.notification.domain.NotificationType;
import dio.budgeting.shared.domain.Money;

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
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class NotificationEntity {
    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "counterpart_name", nullable = false, length = 120)
    private String counterpartName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    Notification toDomain() {
        return new Notification(id, eventId, new AccountId(accountId), type, new Money(amountCents), counterpartName,
                createdAt, readAt);
    }
}
