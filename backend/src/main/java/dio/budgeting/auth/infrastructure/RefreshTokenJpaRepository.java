package dio.budgeting.auth.infrastructure;

import dio.budgeting.auth.domain.RefreshToken;
import dio.budgeting.auth.domain.RefreshTokenRepository;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

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
