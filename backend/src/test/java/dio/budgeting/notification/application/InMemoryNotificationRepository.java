package dio.budgeting.notification.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.notification.domain.Notification;
import dio.budgeting.notification.domain.NotificationRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InMemoryNotificationRepository implements NotificationRepository {
    private final List<Notification> store = new ArrayList<>();

    @Override
    public boolean saveIfAbsent(Notification notification) {
        var exists = store.stream().anyMatch(n ->
                n.eventId().equals(notification.eventId()) && n.accountId().equals(notification.accountId()));
        if (exists) {
            return false;
        }
        store.add(notification);
        return true;
    }

    @Override
    public List<Notification> findLatest(AccountId accountId, int limit) {
        return store.stream()
                .filter(n -> n.accountId().equals(accountId))
                .sorted(Comparator.comparing(Notification::createdAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public long countUnread(AccountId accountId) {
        return store.stream().filter(n -> n.accountId().equals(accountId) && !n.isRead()).count();
    }

    @Override
    public int markAllAsRead(AccountId accountId, Instant now) {
        var marked = 0;
        for (int i = 0; i < store.size(); i++) {
            var n = store.get(i);
            if (n.accountId().equals(accountId) && !n.isRead()) {
                store.set(i, new Notification(n.id(), n.eventId(), n.accountId(), n.type(), n.amount(),
                        n.counterpartName(), n.createdAt(), now));
                marked++;
            }
        }
        return marked;
    }

    public void clear() {
        store.clear();
    }

    public List<Notification> all() {
        return List.copyOf(store);
    }
}
