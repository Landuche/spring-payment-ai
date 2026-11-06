package dio.budgeting.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    /** Lê travando a linha: duas renovações simultâneas do mesmo token não podem ambas ser aceitas. */
    Optional<RefreshToken> findByHashForUpdate(String hash);

    Optional<RefreshToken> findByHash(String hash);

    RefreshToken save(RefreshToken token);

    void revokeFamily(UUID familyId, Instant now);
}
