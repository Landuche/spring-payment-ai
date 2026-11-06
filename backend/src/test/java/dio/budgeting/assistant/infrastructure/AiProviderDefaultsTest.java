package dio.budgeting.assistant.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderDefaultsTest {
    private static final String CHAT = "spring.ai.openai.chat.options.model";
    private static final String TRANSCRIPTION = "spring.ai.openai.audio.transcription.options.model";

    private MockEnvironment aplicar(MockEnvironment env) {
        new AiProviderDefaults().postProcessEnvironment(env, null);
        return env;
    }

    @Test
    void should_useGroqModels_when_baseUrlIsGroq() {
        var env = aplicar(new MockEnvironment().withProperty("AI_BASE_URL", "https://api.groq.com/openai"));

        assertThat(env.getProperty(CHAT)).isEqualTo("openai/gpt-oss-120b");
        assertThat(env.getProperty(TRANSCRIPTION)).isEqualTo("whisper-large-v3");
    }

    @Test
    void should_useOpenAiModels_when_baseUrlIsOpenAiOrMissing() {
        var env = aplicar(new MockEnvironment());

        assertThat(env.getProperty(CHAT)).isEqualTo("gpt-4o-mini");
        assertThat(env.getProperty(TRANSCRIPTION)).isEqualTo("whisper-1");
    }

    @Test
    void should_keepExplicitProperty_when_modelIsConfigured() {
        var env = new MockEnvironment()
                .withProperty("AI_BASE_URL", "https://api.groq.com/openai")
                .withProperty(CHAT, "meu-modelo");
        aplicar(env);

        assertThat(env.getProperty(CHAT)).isEqualTo("meu-modelo");
    }
}
