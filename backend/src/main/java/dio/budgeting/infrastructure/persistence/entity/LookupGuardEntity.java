package dio.budgeting.infrastructure.persistence.entity;

import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.LookupGuard;
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
@Table(name = "lookup_guard")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LookupGuardEntity {
    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    public static LookupGuardEntity from(LookupGuard guard) {
        return new LookupGuardEntity(guard.accountId().uuid(), guard.attempts(), guard.blockedUntil());
    }

    public LookupGuard toDomain() {
        return new LookupGuard(new AccountId(accountId), attempts, blockedUntil);
    }
}
