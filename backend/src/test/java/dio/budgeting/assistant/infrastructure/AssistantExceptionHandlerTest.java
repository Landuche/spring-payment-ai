package dio.budgeting.assistant.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantExceptionHandlerTest {

    private final AssistantExceptionHandler handler = new AssistantExceptionHandler();

    @Test
    void should_answerServiceUnavailable_whenAiProviderKeepsFailingWithTransientError() {
        var response = handler.handleAiUnavailable(new TransientAiException("HTTP 503 - overloaded"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("5");
    }

    @Test
    void should_answerServiceUnavailable_whenProviderIsUnreachable() {
        var response = handler.handleAiUnavailable(new ResourceAccessException("timeout", new IOException()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void should_answerServiceUnavailable_whenProviderRejectsTheRequest() {
        var response = handler.handleAiUnavailable(new NonTransientAiException("HTTP 401 - invalid api key"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void should_showFriendlyMessage_withoutLeakingProviderDetails() {
        var response = handler.handleAiUnavailable(new TransientAiException("HTTP 429 - key sk-secret exceeded quota"));

        var detail = response.getBody().getDetail();
        assertThat(detail).contains("Claudinho").doesNotContain("429").doesNotContain("sk-secret");
    }
}
