package dio.budgeting.assistant.infrastructure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Voz do Claudinho. O app diz qual texto quer ouvir (já resumido para a fala) e recebe o áudio; se a voz por IA
 * estiver desligada ou falhar, ele usa a voz do aparelho.
 */
@RestController
@RequestMapping("/assistant")
public class SpeechController {
    static final int MAX_TEXT_LENGTH = 1000;

    public record SpeechRequest(@NotBlank @Size(max = MAX_TEXT_LENGTH) String text) {
    }

    public record Capabilities(boolean aiSpeech) {
    }

    private final TextToSpeechModel speechModel;
    private final boolean aiSpeech;

    public SpeechController(TextToSpeechModel speechModel,
                            @Value("${budgeting.assistant.ai-speech:true}") boolean aiSpeech) {
        this.speechModel = speechModel;
        this.aiSpeech = aiSpeech;
    }

    @GetMapping("/capabilities")
    public Capabilities capabilities() {
        return new Capabilities(aiSpeech);
    }

    @PostMapping(value = "/speech", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> speak(@Valid @RequestBody SpeechRequest request) {
        if (!aiSpeech) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voz por IA desligada");
        }
        var audio = speechModel.call(request.text());
        if (audio.length == 0) {
            throw new NonTransientAiException("O provedor de voz devolveu um áudio vazio");
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/mpeg")).body(audio);
    }
}
