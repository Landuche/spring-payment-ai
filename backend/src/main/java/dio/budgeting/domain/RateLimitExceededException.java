package dio.budgeting.domain;

import java.time.Instant;

/** Base de todos os bloqueios por excesso de tentativas. Mapeada para HTTP 429 com Retry-After. */
public abstract class RateLimitExceededException extends RuntimeException {
    private final Instant until;

    protected RateLimitExceededException(String message, Instant until) {
        super(message);
        this.until = until;
    }

    public Instant getUntil() {
        return until;
    }
}
