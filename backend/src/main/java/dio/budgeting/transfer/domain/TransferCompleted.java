package dio.budgeting.transfer.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado quando uma transferência é concluída. Os nomes já vão mascarados ("Ana S."): o evento circula
 * pelo broker e não precisa carregar o nome completo de ninguém.
 */
public record TransferCompleted(
        UUID transferId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        long amountCents,
        String sourceName,
        String destinationName,
        Instant occurredAt) {

    public static final String TOPIC = "transfer.completed";
    public static final String TYPE = "TransferCompleted";
}
