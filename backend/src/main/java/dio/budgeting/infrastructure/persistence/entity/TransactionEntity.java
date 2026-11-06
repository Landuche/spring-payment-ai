package dio.budgeting.infrastructure.persistence.entity;

import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Money;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.domain.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TransactionEntity {
    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(nullable = false, length = 120)
    private String description;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "transfer_id")
    private UUID transferId;

    public static TransactionEntity from(Transaction transaction) {
        return new TransactionEntity(
                transaction.id().uuid(),
                transaction.accountId().uuid(),
                transaction.description(),
                transaction.amount().cents(),
                transaction.category(),
                transaction.type(),
                transaction.occurredAt(),
                transaction.createdAt(),
                transaction.transferId());
    }

    public Transaction toDomain() {
        return new Transaction(
                new TransactionId(id),
                new AccountId(accountId),
                description,
                new Money(amountCents),
                category,
                type,
                occurredAt,
                createdAt,
                transferId);
    }
}
