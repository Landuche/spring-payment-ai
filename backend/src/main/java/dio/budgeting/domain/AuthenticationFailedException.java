package dio.budgeting.domain;

/** Credenciais inválidas. A mensagem é genérica de propósito, para não revelar se o e-mail existe. */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
        super("E-mail ou senha inválidos");
    }
}
