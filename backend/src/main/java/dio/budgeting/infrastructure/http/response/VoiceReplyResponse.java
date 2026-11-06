package dio.budgeting.infrastructure.http.response;

import dio.budgeting.application.output.PropostaDeTransferencia;

/** Resposta de voz: o que foi ouvido e a resposta do Claudinho (com a proposta de transferência, se houver). */
public record VoiceReplyResponse(String transcription, String reply, PropostaDeTransferencia transferencia) {
}
