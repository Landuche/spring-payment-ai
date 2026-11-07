package dio.budgeting.notification.infrastructure;

/** Mensagem que nunca vai ser processada, por mais que se tente (ex.: JSON inválido). Vai direto para a fila de falhas. */
class InvalidEventException extends RuntimeException {
    InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
