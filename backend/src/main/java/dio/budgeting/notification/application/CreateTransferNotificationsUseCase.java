package dio.budgeting.notification.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.notification.domain.Notification;
import dio.budgeting.notification.domain.NotificationRepository;
import dio.budgeting.notification.domain.NotificationType;
import dio.budgeting.notification.domain.NotificationsCreated;
import dio.budgeting.outbox.application.OutboxRecorder;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transfer.domain.TransferCompleted;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reage a uma transferência concluída avisando as duas pontas: quem enviou e quem recebeu. Pode ser chamado mais de
 * uma vez com o mesmo evento (entrega "pelo menos uma vez"): o segundo chamado não grava nada nem avisa de novo.
 */
@Service
public class CreateTransferNotificationsUseCase {
    private final NotificationRepository notifications;
    private final OutboxRecorder outbox;

    public CreateTransferNotificationsUseCase(NotificationRepository notifications, OutboxRecorder outbox) {
        this.notifications = notifications;
        this.outbox = outbox;
    }

    @Transactional
    public void execute(TransferCompleted event) {
        var amount = new Money(event.amountCents());
        notify(Notification.unread(event.transferId(), new AccountId(event.sourceAccountId()),
                NotificationType.TRANSFER_SENT, amount, event.destinationName(), event.occurredAt()));
        notify(Notification.unread(event.transferId(), new AccountId(event.destinationAccountId()),
                NotificationType.TRANSFER_RECEIVED, amount, event.sourceName(), event.occurredAt()));
    }

    /** O aviso em tempo real vai pelo outbox, na mesma transação: só sai depois que a notificação foi gravada. */
    private void notify(Notification notification) {
        if (!notifications.saveIfAbsent(notification)) {
            return;
        }
        var accountId = notification.accountId().uuid();
        outbox.record(NotificationsCreated.TOPIC, accountId, NotificationsCreated.TYPE, new NotificationsCreated(accountId));
    }
}
