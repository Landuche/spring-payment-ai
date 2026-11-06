package dio.budgeting.assistant.infrastructure;

import dio.budgeting.assistant.application.PropostaDeTransferencia;


/** Resposta de voz: o que foi ouvido e a resposta do Claudinho (com a proposta de transferência, se houver). */
public record VoiceReplyResponse(String transcription, String reply, PropostaDeTransferencia transferencia) {
}
