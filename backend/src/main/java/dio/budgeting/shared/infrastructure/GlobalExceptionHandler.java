package dio.budgeting.shared.infrastructure;

import dio.budgeting.auth.domain.AuthenticationFailedException;
import dio.budgeting.auth.domain.LoginBlockedException;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.ConflictException;
import dio.budgeting.shared.domain.NotFoundException;
import dio.budgeting.shared.domain.RateLimitExceededException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de negócio violada", ex.getMessage());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ProblemDetail handleAuthenticationFailed(AuthenticationFailedException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Não autorizado", ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Não encontrado", ex.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail handleMissingHeader(MissingRequestHeaderException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Cabeçalho obrigatório", "Cabeçalho " + ex.getHeaderName() + " é obrigatório");
    }

    /** 429 com Retry-After (segundos até o desbloqueio), como manda a RFC 9110. */
    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ProblemDetail> handleRateLimited(RateLimitExceededException ex) {
        var seconds = Math.max(1, Duration.between(Instant.now(), ex.getUntil()).toSeconds());
        var title = ex instanceof LoginBlockedException ? "Muitas tentativas" : "Muitas buscas";
        var problem = problem(HttpStatus.TOO_MANY_REQUESTS, title, ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(problem);
    }

    /**
     * Última linha de defesa contra corrida (ex.: mesma Idempotency-Key em paralelo): a constraint única do banco
     * impede o duplicado, e o cliente recebe 409 em vez de 500. O reenvio com a mesma chave devolve o resultado.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "Conflito de concorrência",
                "Operação concorrente com o mesmo identificador. Repita a requisição com a mesma chave.");
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Conflito", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        var problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "A requisição contém campos inválidos");
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "inválido" : fe.getDefaultMessage(),
                        (first, second) -> first));
        problem.setProperty("fields", fields);
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail handleMalformed(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição malformada",
                "Corpo ou parâmetro inválido (verifique valores de enum e formatos)");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ProblemDetail handleMissingPart(MissingServletRequestPartException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Parte ausente", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
