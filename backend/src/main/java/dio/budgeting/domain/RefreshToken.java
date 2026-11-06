package dio.budgeting.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh token persistido só pelo hash. Uso único: ao renovar, o token é marcado como usado e um novo é emitido
 * na mesma família. Apresentar um token já usado indica roubo e revoga a família inteira.
 */
public record RefreshToken(
        String hash,
        UserId userId,
        UUID familyId,
        Instant expiresAt,
        Instant usedAt,
        Instant revokedAt) {

    public static String hash(String raw) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public RefreshToken marcarComoUsado(Instant now) {
        return new RefreshToken(hash, userId, familyId, expiresAt, now, revokedAt);
    }
}
