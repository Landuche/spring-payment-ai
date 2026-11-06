package dio.budgeting.assistant.infrastructure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param conversationId identifica a conversa para a memória do assistente. Gerado pelo app ao abrir a tela;
 *                       se ausente, usa uma conversa padrão da conta.
 */
public record AssistantCommandRequest(
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 64) String conversationId) {
}
