package dio.budgeting.auth.application;


import dio.budgeting.auth.domain.RefreshToken;
import dio.budgeting.auth.domain.RefreshTokenRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {
    private final Map<String, RefreshToken> store = new HashMap<>();

    @Override
    public Optional<RefreshToken> findByHashForUpdate(String hash) {
        return findByHash(hash);
    }

    @Override
    public Optional<RefreshToken> findByHash(String hash) {
        return Optional.ofNullable(store.get(hash));
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        store.put(token.hash(), token);
        return token;
    }

    @Override
    public void revokeFamily(UUID familyId, Instant now) {
        store.replaceAll((hash, t) -> t.familyId().equals(familyId) && !t.isRevoked()
                ? new RefreshToken(t.hash(), t.userId(), t.familyId(), t.expiresAt(), t.usedAt(), now)
                : t);
    }
}
