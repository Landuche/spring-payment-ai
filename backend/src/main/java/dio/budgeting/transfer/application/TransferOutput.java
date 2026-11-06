package dio.budgeting.transfer.application;

import java.math.BigDecimal;
import java.time.Instant;

/** {@code replayed = true} indica que a Idempotency-Key já tinha sido processada: nada foi debitado de novo. */
public record TransferOutput(String id, BigDecimal amount, String description, Instant createdAt, boolean replayed) {
}
