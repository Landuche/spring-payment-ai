package dio.budgeting.shared.domain;

/** Mapeado para HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
