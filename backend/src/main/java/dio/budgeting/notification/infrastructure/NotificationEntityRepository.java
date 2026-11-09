package dio.budgeting.notification.infrastructure;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface NotificationEntityRepository extends CrudRepository<NotificationEntity, UUID> {

    @Modifying
    @Query(value = """
            insert into notification (id, event_id, account_id, type, amount_cents, counterpart_name, created_at)
            values (:id, :eventId, :accountId, :type, :amountCents, :counterpartName, :createdAt)
            on conflict (event_id, account_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("eventId") UUID eventId, @Param("accountId") UUID accountId,
                       @Param("type") String type, @Param("amountCents") long amountCents,
                       @Param("counterpartName") String counterpartName, @Param("createdAt") Instant createdAt);

    List<NotificationEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Limit limit);

    long countByAccountIdAndReadAtIsNull(UUID accountId);

    @Modifying
    @Query("update NotificationEntity n set n.readAt = :now where n.accountId = :accountId and n.readAt is null")
    int markAllAsRead(@Param("accountId") UUID accountId, @Param("now") Instant now);
}
