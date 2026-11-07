package dio.budgeting.notification.application;

import dio.budgeting.account.application.CurrentAccountProvider;
import dio.budgeting.notification.domain.NotificationRepository;

import org.springframework.stereotype.Service;

@Service
public class ListNotificationsUseCase {
    static final int LIMIT = 50;

    private final NotificationRepository notifications;
    private final CurrentAccountProvider currentAccount;

    public ListNotificationsUseCase(NotificationRepository notifications, CurrentAccountProvider currentAccount) {
        this.notifications = notifications;
        this.currentAccount = currentAccount;
    }

    public NotificationsOutput execute() {
        var account = currentAccount.current();
        var items = notifications.findLatest(account, LIMIT).stream().map(NotificationOutput::from).toList();
        return new NotificationsOutput(items, notifications.countUnread(account));
    }
}
