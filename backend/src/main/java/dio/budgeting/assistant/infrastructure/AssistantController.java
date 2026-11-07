package dio.budgeting.assistant.infrastructure;

import jakarta.validation.Valid;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Porta de entrada HTTP do assistente (Claudinho). A conversa em si fica no {@link AssistantChat}.
 * <p>
 * A resposta falada fica por conta do app (voz do dispositivo); o backend só transcreve e responde em texto.
 */
@RestController
@RequestMapping("/assistant")
public class AssistantController {
    private final AssistantChat chat;
    private final TranscriptionModel transcriptionModel;

    public AssistantController(AssistantChat chat, TranscriptionModel transcriptionModel) {
        this.chat = chat;
        this.transcriptionModel = transcriptionModel;
    }

    @PostMapping(value = "/text", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AssistantReplyResponse text(@Valid @RequestBody AssistantCommandRequest request) {
        return chat.reply(request.message(), request.conversationId());
    }

    /**
     * O app avisa o desfecho do cartão de transferência (enviada, cancelada ou com falha) e o Claudinho responde na
     * mesma conversa, com memória, propondo o próximo passo. Não executa nada: a transferência já foi tratada pelo app.
     */
    @PostMapping(value = "/event", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AssistantReplyResponse event(@Valid @RequestBody AssistantEventRequest request) {
        return chat.reply(AppNotices.message(request), request.conversationId());
    }

    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public VoiceReplyResponse voice(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "conversationId", required = false) String conversationId) {
        var transcription = transcriptionModel.transcribe(file.getResource());
        var reply = chat.reply(transcription, conversationId);
        return new VoiceReplyResponse(transcription, reply.reply(), reply.transfer());
    }
}
