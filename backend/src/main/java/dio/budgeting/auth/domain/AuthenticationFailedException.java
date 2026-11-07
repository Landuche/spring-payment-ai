package dio.budgeting.auth.domain;

/** mensagem é genérica para não revelar se o e-mail existe. */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
        super("E-mail ou senha inválidos");
    }
}
