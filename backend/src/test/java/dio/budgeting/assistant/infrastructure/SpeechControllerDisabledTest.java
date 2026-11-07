package dio.budgeting.assistant.infrastructure;

import dio.budgeting.account.infrastructure.SpringCurrentAccountProvider;
import dio.budgeting.auth.infrastructure.SecurityConfig;
import dio.budgeting.shared.infrastructure.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Com AI_SPEECH=false o servidor não gera áudio, e o app lê as respostas com a voz do aparelho. */
@WebMvcTest(controllers = SpeechController.class,
        properties = {"budgeting.security.jwt-secret=test-secret-com-pelo-menos-32-bytes-123456",
                "budgeting.assistant.ai-speech=false"})
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, AssistantExceptionHandler.class, SecurityConfig.class,
        SpringCurrentAccountProvider.class})
class SpeechControllerDisabledTest {

    private static final String ACCOUNT = "6f1d2c3a-1111-4a5b-8c9d-0e1f2a3b4c5d";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TextToSpeechModel speechModel;

    @Test
    void should_tellTheAppThatAiSpeechIsOff() throws Exception {
        mvc.perform(get("/assistant/capabilities").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiSpeech").value(false));
    }

    @Test
    void should_notGenerateAudio_andNeverCallTheProvider() throws Exception {
        mvc.perform(post("/assistant/speech").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Olá\"}"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(speechModel);
    }
}
