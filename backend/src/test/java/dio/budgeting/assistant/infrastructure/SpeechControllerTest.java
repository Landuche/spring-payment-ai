package dio.budgeting.assistant.infrastructure;

import dio.budgeting.account.infrastructure.SpringCurrentAccountProvider;
import dio.budgeting.auth.infrastructure.SecurityConfig;
import dio.budgeting.shared.infrastructure.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpeechController.class,
        properties = {"budgeting.security.jwt-secret=test-secret-com-pelo-menos-32-bytes-123456",
                "budgeting.assistant.ai-speech=true"})
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, AssistantExceptionHandler.class, SecurityConfig.class,
        SpringCurrentAccountProvider.class})
class SpeechControllerTest {

    private static final String ACCOUNT = "6f1d2c3a-1111-4a5b-8c9d-0e1f2a3b4c5d";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TextToSpeechModel speechModel;

    @Test
    void should_returnTheAudioOfTheText() throws Exception {
        var audio = new byte[] {1, 2, 3, 4};
        when(speechModel.call("Seu saldo é de 50 reais.")).thenReturn(audio);

        mvc.perform(post("/assistant/speech").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Seu saldo é de 50 reais.\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "audio/mpeg"))
                .andExpect(content().bytes(audio));
    }

    @Test
    void should_rejectAnEmptyText_withoutCallingTheProvider() throws Exception {
        mvc.perform(post("/assistant/speech").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(speechModel);
    }

    @Test
    void should_rejectATextLongerThanTheLimit() throws Exception {
        var longText = "a".repeat(SpeechController.MAX_TEXT_LENGTH + 1);

        mvc.perform(post("/assistant/speech").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + longText + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(speechModel);
    }

    @Test
    void should_answerServiceUnavailable_whenTheVoiceProviderFails() throws Exception {
        when(speechModel.call("Olá")).thenThrow(new TransientAiException("HTTP 503 - fora do ar"));

        mvc.perform(post("/assistant/speech").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Olá\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void should_answerServiceUnavailable_whenTheProviderReturnsNoAudio() throws Exception {
        when(speechModel.call("Olá")).thenReturn(new byte[0]);

        mvc.perform(post("/assistant/speech").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Olá\"}"))
                .andExpect(status().isServiceUnavailable());

        verify(speechModel).call("Olá");
    }

    @Test
    void should_requireAuthentication() throws Exception {
        mvc.perform(post("/assistant/speech").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Olá\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/assistant/capabilities")).andExpect(status().isUnauthorized());
    }

    @Test
    void should_tellTheAppThatAiSpeechIsAvailable() throws Exception {
        mvc.perform(get("/assistant/capabilities").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiSpeech").value(true));
    }
}
