package dio.budgeting.assistant.infrastructure;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Escolhe os modelos de chat e de transcrição conforme o provedor configurado em {@code AI_BASE_URL}
 */
public class AiProviderDefaults implements EnvironmentPostProcessor {
    static final String SOURCE_NAME = "aiProviderDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String baseUrl = environment.getProperty("AI_BASE_URL", "");
        boolean groq = baseUrl.toLowerCase().contains("groq");

        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, Map.of(
                "spring.ai.openai.chat.options.model", groq ? "openai/gpt-oss-120b" : "gpt-4o-mini",
                "spring.ai.openai.audio.transcription.options.model", groq ? "whisper-large-v3" : "whisper-1")));
    }
}
