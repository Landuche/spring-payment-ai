package dio.budgeting.assistant.infrastructure;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;

/**
 * Escolhe os modelos de chat, de transcrição e de voz conforme o provedor configurado em {@code AI_BASE_URL}.
 * Com a OpenAI, a voz vem da própria OpenAI, e com a Groq, que não tem voz em português, vem do Kokoro.
 */
public class AiProviderDefaults implements EnvironmentPostProcessor {
    static final String SOURCE_NAME = "aiProviderDefaults";
    static final String DEFAULT_KOKORO_URL = "http://localhost:8880";

    private static final String SPEECH = "spring.ai.openai.audio.speech.";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String baseUrl = environment.getProperty("AI_BASE_URL", "");
        boolean groq = baseUrl.toLowerCase().contains("groq");

        var defaults = new HashMap<String, Object>();
        defaults.put("spring.ai.openai.chat.options.model", groq ? "openai/gpt-oss-120b" : "gpt-4o-mini");
        defaults.put("spring.ai.openai.audio.transcription.options.model", groq ? "whisper-large-v3" : "gpt-4o-transcribe");
        if (groq) {
            defaults.put(SPEECH + "base-url", environment.getProperty("KOKORO_URL", DEFAULT_KOKORO_URL));
            // O Kokoro não confere a chave, mas o cliente exige uma. Não vale mandar a chave da Groq para ele.
            defaults.put(SPEECH + "api-key", "kokoro");
            defaults.put(SPEECH + "options.model", "kokoro");
            defaults.put(SPEECH + "options.voice", "pm_alex");
            defaults.put(SPEECH + "options.speed", 1.0);
        } else {
            defaults.put(SPEECH + "options.model", "gpt-4o-mini-tts");
            defaults.put(SPEECH + "options.voice", "onyx");
            defaults.put(SPEECH + "options.speed", 1.2);
        }
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, defaults));
    }
}
