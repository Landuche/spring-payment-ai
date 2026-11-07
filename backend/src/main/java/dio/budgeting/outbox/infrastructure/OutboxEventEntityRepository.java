package dio.budgeting.outbox.infrastructure;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface OutboxEventEntityRepository extends CrudRepository<OutboxEventEntity, UUID> {

    /** SKIP LOCKED: quem chega depois pula as linhas que outra instância já está publicando. */
    @Query(value = """
            select * from outbox_event
            where published_at is null
            order by created_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEventEntity> findPendingForUpdate(@Param("limit") int limit);
}
