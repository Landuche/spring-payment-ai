package dio.budgeting.notification.application;

import dio.budgeting.account.application.CurrentAccountProvider;
import dio.budgeting.notification.domain.NotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class MarkNotificationsReadUseCase {
    private final NotificationRepository notifications;
    private final CurrentAccountProvider currentAccount;
    private final Clock clock;

    public MarkNotificationsReadUseCase(NotificationRepository notifications, CurrentAccountProvider currentAccount,
                                        Clock clock) {
        this.notifications = notifications;
        this.currentAccount = currentAccount;
        this.clock = clock;
    }

    @Transactional
    public void execute() {
        notifications.markAllAsRead(currentAccount.current(), Instant.now(clock));
    }
}
