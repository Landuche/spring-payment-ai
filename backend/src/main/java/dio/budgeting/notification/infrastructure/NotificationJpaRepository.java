package dio.budgeting.notification.infrastructure;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.notification.domain.Notification;
import dio.budgeting.notification.domain.NotificationRepository;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
class NotificationJpaRepository implements NotificationRepository {
    private final NotificationEntityRepository entityRepository;

    NotificationJpaRepository(NotificationEntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Override
    public boolean saveIfAbsent(Notification notification) {
        return entityRepository.insertIfAbsent(notification.id(), notification.eventId(),
                notification.accountId().uuid(), notification.type().name(), notification.amount().cents(),
                notification.counterpartName(), notification.createdAt()) == 1;
    }

    @Override
    public List<Notification> findLatest(AccountId accountId, int limit) {
        return entityRepository.findByAccountIdOrderByCreatedAtDesc(accountId.uuid(), Limit.of(limit)).stream()
                .map(NotificationEntity::toDomain)
                .toList();
    }

    @Override
    public long countUnread(AccountId accountId) {
        return entityRepository.countByAccountIdAndReadAtIsNull(accountId.uuid());
    }

    @Override
    public int markAllAsRead(AccountId accountId, Instant now) {
        return entityRepository.markAllAsRead(accountId.uuid(), now);
    }
}
