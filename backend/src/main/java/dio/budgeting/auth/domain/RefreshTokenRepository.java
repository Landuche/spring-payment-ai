package dio.budgeting.auth.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    /** previne duas renovações simultâneas do mesmo token. */
    Optional<RefreshToken> findByHashForUpdate(String hash);

    Optional<RefreshToken> findByHash(String hash);

    RefreshToken save(RefreshToken token);

    void revokeFamily(UUID familyId, Instant now);

    /** Remove tokens expirados. */
    int deleteExpired(Instant now);
}
