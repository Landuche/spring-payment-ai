package dio.budgeting.domain;

import java.time.Instant;

/** Login bloqueado por excesso de falhas (por e-mail ou por IP). A mensagem não revela qual chave bloqueou. */
public class LoginBlockedException extends RateLimitExceededException {
    public LoginBlockedException(Instant until) {
        super("Muitas tentativas de acesso. Tente novamente mais tarde.", until);
    }
}
