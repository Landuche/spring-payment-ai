package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.RefreshToken;
import dio.budgeting.domain.RefreshTokenRepository;
import dio.budgeting.domain.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class RefreshTokenEntity {
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    static RefreshTokenEntity from(RefreshToken token) {
        return new RefreshTokenEntity(token.hash(), token.userId().uuid(), token.familyId(),
                token.expiresAt(), token.usedAt(), token.revokedAt());
    }

    RefreshToken toDomain() {
        return new RefreshToken(tokenHash, new UserId(userId), familyId, expiresAt, usedAt, revokedAt);
    }
}

interface RefreshTokenEntityRepository extends CrudRepository<RefreshTokenEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshTokenEntity t where t.tokenHash = :hash")
    Optional<RefreshTokenEntity> lockByHash(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :now where t.familyId = :family and t.revokedAt is null")
    int revokeFamily(@Param("family") UUID family, @Param("now") Instant now);

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.expiresAt < :agora")
    int apagarExpirados(@Param("agora") Instant agora);
}

@Repository
class RefreshTokenJpaRepository implements RefreshTokenRepository {
    private final RefreshTokenEntityRepository entityRepository;

    RefreshTokenJpaRepository(RefreshTokenEntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Override
    public Optional<RefreshToken> findByHashForUpdate(String hash) {
        return entityRepository.lockByHash(hash).map(RefreshTokenEntity::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByHash(String hash) {
        return entityRepository.findById(hash).map(RefreshTokenEntity::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return entityRepository.save(RefreshTokenEntity.from(token)).toDomain();
    }

    @Override
    public void revokeFamily(UUID familyId, Instant now) {
        entityRepository.revokeFamily(familyId, now);
    }
}
