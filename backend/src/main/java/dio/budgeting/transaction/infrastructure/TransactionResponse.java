package dio.budgeting.transaction.infrastructure;


import dio.budgeting.transaction.application.TransactionOutput;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(
        String id,
        String description,
        String category,
        String type,
        BigDecimal amount,
        LocalDate occurredAt,
        Instant createdAt) {

    public static TransactionResponse from(TransactionOutput output) {
        return new TransactionResponse(
                output.id(),
                output.description(),
                output.category(),
                output.type(),
                output.amount(),
                output.occurredAt(),
                output.createdAt());
    }
}
