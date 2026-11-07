package dio.budgeting.auth.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenEntityRepository extends CrudRepository<RefreshTokenEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshTokenEntity t where t.tokenHash = :hash")
    Optional<RefreshTokenEntity> lockByHash(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :now where t.familyId = :family and t.revokedAt is null")
    int revokeFamily(@Param("family") UUID family, @Param("now") Instant now);

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
