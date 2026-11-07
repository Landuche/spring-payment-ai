package dio.budgeting.notification.infrastructure;

import dio.budgeting.notification.application.ListNotificationsUseCase;
import dio.budgeting.notification.application.MarkNotificationsReadUseCase;
import dio.budgeting.notification.application.NotificationsOutput;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final ListNotificationsUseCase listNotifications;
    private final MarkNotificationsReadUseCase markNotificationsRead;

    public NotificationController(ListNotificationsUseCase listNotifications,
                                  MarkNotificationsReadUseCase markNotificationsRead) {
        this.listNotifications = listNotifications;
        this.markNotificationsRead = markNotificationsRead;
    }

    @GetMapping
    public NotificationsOutput list() {
        return listNotifications.execute();
    }

    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead() {
        markNotificationsRead.execute();
    }
}
