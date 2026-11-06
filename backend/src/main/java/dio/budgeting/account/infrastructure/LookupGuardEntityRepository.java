package dio.budgeting.account.infrastructure;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface LookupGuardEntityRepository extends CrudRepository<LookupGuardEntity, UUID> {

    /** Só remove contadores zerados e sem bloqueio: sem eles, a linha equivale a um registro novo. */
    @Modifying
    @Query("""
            delete from LookupGuardEntity g
            where g.attempts = 0
              and (g.blockedUntil is null or g.blockedUntil < :agora)
            """)
    int apagarSemPendencia(@Param("agora") Instant agora);
}
