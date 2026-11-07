package dio.budgeting.auth.domain;

import dio.budgeting.shared.domain.RateLimitExceededException;

import java.time.Instant;

/** Login bloqueado por excesso de falhas para um e-mail. */
public class LoginBlockedException extends RateLimitExceededException {
    public LoginBlockedException(Instant until) {
        super("Muitas tentativas de acesso. Tente novamente mais tarde.", until);
    }
}
