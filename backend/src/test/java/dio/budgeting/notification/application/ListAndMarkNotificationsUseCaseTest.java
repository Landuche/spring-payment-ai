package dio.budgeting.notification.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.notification.domain.Notification;
import dio.budgeting.notification.domain.NotificationType;
import dio.budgeting.shared.domain.Money;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ListAndMarkNotificationsUseCaseTest {

    private final Instant now = Instant.parse("2026-11-08T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
    private final AccountId me = new AccountId();
    private final AccountId other = new AccountId();

    private final ListNotificationsUseCase list = new ListNotificationsUseCase(notifications, () -> me);
    private final MarkNotificationsReadUseCase markRead = new MarkNotificationsReadUseCase(notifications, () -> me, clock);

    @Test
    void should_listOnlyMyNotifications_newestFirst_withTheUnreadCount() {
        add(me, "mais antiga", 1);
        add(me, "mais nova", 3);
        add(me, "do meio", 2);
        add(other, "de outra conta", 4);

        var output = list.execute();

        assertThat(output.items()).extracting(NotificationOutput::counterpartName)
                .containsExactly("mais nova", "do meio", "mais antiga");
        assertThat(output.unread()).isEqualTo(3);
    }

    @Test
    void should_exposeTheFieldsTheAppNeeds() {
        add(me, "Bruno L.", 1);

        var item = list.execute().items().get(0);

        assertThat(item.type()).isEqualTo("TRANSFER_RECEIVED");
        assertThat(item.amount()).isEqualByComparingTo("10.00");
        assertThat(item.read()).isFalse();
    }

    @Test
    void should_countAllUnread_evenWhenTheListIsLimited() {
        for (int i = 0; i < ListNotificationsUseCase.LIMIT + 5; i++) {
            add(me, "n" + i, i);
        }

        var output = list.execute();

        assertThat(output.items()).hasSize(ListNotificationsUseCase.LIMIT);
        assertThat(output.unread()).isEqualTo(ListNotificationsUseCase.LIMIT + 5);
    }

    @Test
    void should_markOnlyMyNotificationsAsRead() {
        add(me, "a", 1);
        add(me, "b", 2);
        add(other, "c", 3);

        markRead.execute();

        assertThat(notifications.countUnread(me)).isZero();
        assertThat(notifications.countUnread(other)).isEqualTo(1);
        assertThat(list.execute().items()).allMatch(NotificationOutput::read);
    }

    @Test
    void should_keepNotificationsThatArriveAfterMarkingAsRead_unread() {
        add(me, "antiga", 1);
        markRead.execute();

        add(me, "nova", 2);

        assertThat(list.execute().unread()).isEqualTo(1);
    }

    private void add(AccountId account, String counterpart, int secondsAfterStart) {
        notifications.saveIfAbsent(Notification.unread(UUID.randomUUID(), account, NotificationType.TRANSFER_RECEIVED,
                new Money(1000), counterpart, now.minusSeconds(10_000).plusSeconds(secondsAfterStart)));
    }
}
