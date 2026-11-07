package dio.budgeting.assistant.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderDefaultsTest {
    private static final String CHAT = "spring.ai.openai.chat.options.model";
    private static final String TRANSCRIPTION = "spring.ai.openai.audio.transcription.options.model";
    private static final String SPEECH_MODEL = "spring.ai.openai.audio.speech.options.model";
    private static final String SPEECH_VOICE = "spring.ai.openai.audio.speech.options.voice";
    private static final String SPEECH_URL = "spring.ai.openai.audio.speech.base-url";
    private static final String SPEECH_KEY = "spring.ai.openai.audio.speech.api-key";

    private MockEnvironment apply(MockEnvironment env) {
        new AiProviderDefaults().postProcessEnvironment(env, null);
        return env;
    }

    @Test
    void should_useGroqModels_when_baseUrlIsGroq() {
        var env = apply(new MockEnvironment().withProperty("AI_BASE_URL", "https://api.groq.com/openai"));

        assertThat(env.getProperty(CHAT)).isEqualTo("openai/gpt-oss-120b");
        assertThat(env.getProperty(TRANSCRIPTION)).isEqualTo("whisper-large-v3");
    }

    @Test
    void should_useOpenAiModels_when_baseUrlIsOpenAiOrMissing() {
        var env = apply(new MockEnvironment());

        assertThat(env.getProperty(CHAT)).isEqualTo("gpt-4o-mini");
        assertThat(env.getProperty(TRANSCRIPTION)).isEqualTo("whisper-1");
    }

    @Test
    void should_keepExplicitProperty_when_modelIsConfigured() {
        var env = new MockEnvironment()
                .withProperty("AI_BASE_URL", "https://api.groq.com/openai")
                .withProperty(CHAT, "meu-modelo");
        apply(env);

        assertThat(env.getProperty(CHAT)).isEqualTo("meu-modelo");
    }

    @Test
    void should_speakWithKokoroOnTheLocalServer_when_baseUrlIsGroq() {
        var env = apply(new MockEnvironment().withProperty("AI_BASE_URL", "https://api.groq.com/openai"));

        assertThat(env.getProperty(SPEECH_URL)).isEqualTo("http://localhost:8880");
        assertThat(env.getProperty(SPEECH_MODEL)).isEqualTo("kokoro");
        assertThat(env.getProperty(SPEECH_VOICE)).isEqualTo("pf_dora");
        assertThat(env.getProperty(SPEECH_KEY)).isEqualTo("kokoro");
    }

    @Test
    void should_speakWithOpenAi_whenBaseUrlIsOpenAi_withoutOverridingTheConnection() {
        var env = apply(new MockEnvironment().withProperty("AI_BASE_URL", "https://api.openai.com"));

        assertThat(env.getProperty(SPEECH_MODEL)).isEqualTo("gpt-4o-mini-tts");
        assertThat(env.getProperty(SPEECH_VOICE)).isEqualTo("nova");
        assertThat(env.getProperty(SPEECH_URL)).isNull();
        assertThat(env.getProperty(SPEECH_KEY)).isNull();
    }

    @Test
    void should_useTheKokoroAddressTheComposeProvides_onlyWhenTheProviderIsGroq() {
        var groq = apply(new MockEnvironment()
                .withProperty("AI_BASE_URL", "https://api.groq.com/openai")
                .withProperty("KOKORO_URL", "http://kokoro:8880"));
        var openAi = apply(new MockEnvironment()
                .withProperty("AI_BASE_URL", "https://api.openai.com")
                .withProperty("KOKORO_URL", "http://kokoro:8880"));

        assertThat(groq.getProperty(SPEECH_URL)).isEqualTo("http://kokoro:8880");
        assertThat(openAi.getProperty(SPEECH_URL)).isNull();
    }
}
