package dio.budgeting.assistant.infrastructure;

import dio.budgeting.transfer.application.TransferProposal;

/** Resposta de voz. */
public record VoiceReplyResponse(String transcription, String reply, TransferProposal transfer) {
}
