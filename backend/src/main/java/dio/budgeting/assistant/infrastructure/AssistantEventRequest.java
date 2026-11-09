package dio.budgeting.assistant.infrastructure;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Avisa o Claudinho como terminou a transferência do cartão: confirmada, cancelada ou com falha.
 */
public record AssistantEventRequest(
        @NotNull Type type,
        @NotBlank @Size(max = 60) String recipientName,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @Size(max = 200) String reason,
        @Size(max = 64) String conversationId) {

    public enum Type {
        TRANSFER_CONFIRMED,
        TRANSFER_CANCELED,
        TRANSFER_FAILED
    }
}
