package dio.budgeting.infrastructure.http.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Aviso do app ao Claudinho sobre o desfecho de uma proposta dele (o cartão de transferência). O app já executou ou
 * cancelou a operação; isto só deixa o Claudinho saber, para responder e propor o próximo passo.
 */
public record AssistantEventRequest(
        @NotNull Tipo tipo,
        @NotBlank @Size(max = 60) String nomeDestinatario,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal valor,
        @Size(max = 200) String motivo,
        @Size(max = 64) String conversationId) {

    public enum Tipo {
        TRANSFERENCIA_CONFIRMADA,
        TRANSFERENCIA_CANCELADA,
        TRANSFERENCIA_FALHOU
    }
}
