package dio.budgeting.assistant.infrastructure;

import dio.budgeting.transfer.application.TransferProposal;

/**
 * Resposta do Claudinho. {@code transfer} é preenchida quando ele preparou uma transferência: o app mostra um
 * cartão de confirmação, e só o toque do cliente a executa.
 */
public record AssistantReplyResponse(String reply, TransferProposal transfer) {
}
