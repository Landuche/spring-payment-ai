package dio.budgeting.notification.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.notification.domain.NotificationType;
import dio.budgeting.transfer.domain.TransferCompleted;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTransferNotificationsUseCaseTest {

    private final InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
    private final CreateTransferNotificationsUseCase useCase = new CreateTransferNotificationsUseCase(notifications);

    private final AccountId source = new AccountId();
    private final AccountId destination = new AccountId();
    private final TransferCompleted event = new TransferCompleted(UUID.randomUUID(), source.uuid(), destination.uuid(),
            3050, "Ana S.", "Bruno L.", Instant.parse("2026-11-08T12:00:00Z"));

    @Test
    void should_notifyTheSender_withTheRecipientAsCounterpart() {
        useCase.execute(event);

        var sent = notifications.findLatest(source, 10);
        assertThat(sent).singleElement().satisfies(n -> {
            assertThat(n.type()).isEqualTo(NotificationType.TRANSFER_SENT);
            assertThat(n.counterpartName()).isEqualTo("Bruno L.");
            assertThat(n.amount().cents()).isEqualTo(3050);
            assertThat(n.createdAt()).isEqualTo(event.occurredAt());
            assertThat(n.isRead()).isFalse();
        });
    }

    @Test
    void should_notifyTheReceiver_withTheSenderAsCounterpart() {
        useCase.execute(event);

        var received = notifications.findLatest(destination, 10);
        assertThat(received).singleElement().satisfies(n -> {
            assertThat(n.type()).isEqualTo(NotificationType.TRANSFER_RECEIVED);
            assertThat(n.counterpartName()).isEqualTo("Ana S.");
            assertThat(n.amount().cents()).isEqualTo(3050);
        });
    }

    @Test
    void should_beIdempotent_whenTheSameEventIsDeliveredTwice() {
        useCase.execute(event);
        useCase.execute(event);

        assertThat(notifications.all()).hasSize(2);
        assertThat(notifications.countUnread(source)).isEqualTo(1);
        assertThat(notifications.countUnread(destination)).isEqualTo(1);
    }

    @Test
    void should_createSeparateNotifications_forDifferentTransfers() {
        useCase.execute(event);
        useCase.execute(new TransferCompleted(UUID.randomUUID(), source.uuid(), destination.uuid(), 100,
                "Ana S.", "Bruno L.", Instant.parse("2026-11-08T13:00:00Z")));

        assertThat(notifications.all()).hasSize(4);
    }
}
