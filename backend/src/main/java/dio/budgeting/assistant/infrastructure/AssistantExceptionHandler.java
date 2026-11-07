package dio.budgeting.assistant.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
public class AssistantExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(AssistantExceptionHandler.class);

    /**
     * Provedor de IA fora do ar, sem resposta ou no limite de uso, depois de esgotadas as tentativas do Spring AI.
     * O cliente recebe uma mensagem amigável; o detalhe do provedor (que pode citar chave e cotas) fica só no log.
     */
    @ExceptionHandler({TransientAiException.class, NonTransientAiException.class, ResourceAccessException.class})
    ResponseEntity<ProblemDetail> handleAiUnavailable(RuntimeException ex) {
        log.warn("Provedor de IA indisponível mesmo após as tentativas: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "O Claudinho não conseguiu responder agora. Tente de novo em alguns instantes.");
        problem.setTitle("Assistente indisponível");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(problem);
    }
}
