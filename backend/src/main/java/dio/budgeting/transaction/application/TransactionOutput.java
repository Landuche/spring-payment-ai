package dio.budgeting.transaction.application;


import dio.budgeting.transaction.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** {@code createdAt} é o momento exato do lançamento; nulo quando só a data é conhecida. */
public record TransactionOutput(
        String id,
        String description,
        String category,
        String type,
        BigDecimal amount,
        LocalDate occurredAt,
        Instant createdAt) {

    public static TransactionOutput from(Transaction transaction) {
        return new TransactionOutput(
                transaction.id().uuid().toString(),
                transaction.description(),
                transaction.category().name(),
                transaction.type().name(),
                transaction.amount().toReais(),
                transaction.occurredAt(),
                transaction.createdAt());
    }
}
