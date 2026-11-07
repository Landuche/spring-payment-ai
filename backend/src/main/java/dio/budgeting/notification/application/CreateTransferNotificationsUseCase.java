package dio.budgeting.notification.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.notification.domain.Notification;
import dio.budgeting.notification.domain.NotificationRepository;
import dio.budgeting.notification.domain.NotificationType;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transfer.domain.TransferCompleted;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reage a uma transferência concluída avisando as duas pontas: quem enviou e quem recebeu. Pode ser chamado mais de
 * uma vez com o mesmo evento (entrega "pelo menos uma vez"): o segundo chamado não grava nada.
 */
@Service
public class CreateTransferNotificationsUseCase {
    private final NotificationRepository notifications;

    public CreateTransferNotificationsUseCase(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional
    public void execute(TransferCompleted event) {
        var amount = new Money(event.amountCents());
        notifications.saveIfAbsent(Notification.unread(event.transferId(), new AccountId(event.sourceAccountId()),
                NotificationType.TRANSFER_SENT, amount, event.destinationName(), event.occurredAt()));
        notifications.saveIfAbsent(Notification.unread(event.transferId(), new AccountId(event.destinationAccountId()),
                NotificationType.TRANSFER_RECEIVED, amount, event.sourceName(), event.occurredAt()));
    }
}
