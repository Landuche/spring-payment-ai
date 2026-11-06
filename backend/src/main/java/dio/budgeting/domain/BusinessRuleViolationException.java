package dio.budgeting.domain;

/**
 * Violação de regra de negócio. Mapeada para HTTP 422 na camada de infraestrutura.
 */
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
