package dio.budgeting.shared.domain;

/** Conflito com estado existente (ex: e-mail já cadastrado). Mapeado para HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
