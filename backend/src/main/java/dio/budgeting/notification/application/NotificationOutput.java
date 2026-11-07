package dio.budgeting.notification.application;

import dio.budgeting.notification.domain.Notification;

import java.math.BigDecimal;
import java.time.Instant;

public record NotificationOutput(
        String id,
        String type,
        BigDecimal amount,
        String counterpartName,
        Instant createdAt,
        boolean read) {

    public static NotificationOutput from(Notification notification) {
        return new NotificationOutput(
                notification.id().toString(),
                notification.type().name(),
                notification.amount().toReais(),
                notification.counterpartName(),
                notification.createdAt(),
                notification.isRead());
    }
}
