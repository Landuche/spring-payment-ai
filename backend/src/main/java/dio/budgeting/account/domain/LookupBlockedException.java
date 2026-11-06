package dio.budgeting.account.domain;

import dio.budgeting.shared.domain.RateLimitExceededException;

import java.time.Instant;

/** Conta bloqueada por excesso de buscas de destinatário. */
public class LookupBlockedException extends RateLimitExceededException {
    public LookupBlockedException(Instant until) {
        super("Limite de buscas atingido. Tente novamente mais tarde.", until);
    }
}
