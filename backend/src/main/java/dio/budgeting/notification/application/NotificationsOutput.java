package dio.budgeting.notification.application;

import java.util.List;

/** {@code unread} conta todos os não lidos da conta, mesmo que a lista traga só os mais recentes. */
public record NotificationsOutput(List<NotificationOutput> items, long unread) {
}
