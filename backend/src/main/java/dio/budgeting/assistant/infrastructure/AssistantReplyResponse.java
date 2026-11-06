package dio.budgeting.assistant.infrastructure;

import dio.budgeting.assistant.application.PropostaDeTransferencia;


/**
 * Resposta do Claudinho. {@code transferencia} é preenchida quando ele preparou uma transferência: o app mostra um
 * cartão de confirmação, e só o toque do cliente a executa.
 */
public record AssistantReplyResponse(String reply, PropostaDeTransferencia transferencia) {
}
