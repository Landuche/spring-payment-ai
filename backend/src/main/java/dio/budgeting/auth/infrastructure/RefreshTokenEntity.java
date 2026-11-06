package dio.budgeting.auth.infrastructure;

import dio.budgeting.auth.domain.RefreshToken;
import dio.budgeting.auth.domain.UserId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefreshTokenEntity {
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
