package dio.budgeting.auth.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

interface FailureCounterEntityRepository extends CrudRepository<FailureCounterEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from FailureCounterEntity c where c.counterKey = :key")
    Optional<FailureCounterEntity> lockByKey(@Param("key") String key);

    @Modifying
    @Query("""
            delete from FailureCounterEntity c
            where (c.blockedUntil is null or c.blockedUntil < :cutoff)
              and (c.windowStart is null or c.windowStart < :cutoff)
            """)
    int deleteInactive(@Param("cutoff") Instant cutoff);
}
