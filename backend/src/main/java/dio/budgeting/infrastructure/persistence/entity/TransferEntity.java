package dio.budgeting.infrastructure.persistence.entity;

import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.Money;
import dio.budgeting.domain.Transfer;
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
@Table(name = "transfers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TransferEntity {
    @Id
    private UUID id;

    @Column(name = "origin_account_id", nullable = false)
    private UUID originAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(length = 120)
    private String description;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static TransferEntity from(Transfer transfer) {
        return new TransferEntity(
                transfer.id(),
                transfer.origin().uuid(),
                transfer.destination().uuid(),
                transfer.amount().cents(),
                transfer.description(),
                transfer.idempotencyKey(),
                transfer.createdAt());
    }

    public Transfer toDomain() {
        return new Transfer(id, new AccountId(originAccountId), new AccountId(destinationAccountId),
                new Money(amountCents), description, idempotencyKey, createdAt);
    }
}
