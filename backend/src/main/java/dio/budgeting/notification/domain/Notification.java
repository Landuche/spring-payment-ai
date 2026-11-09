package dio.budgeting.notification.domain;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.shared.domain.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aviso para uma conta sobre algo que aconteceu nela. Guarda dados estruturados (tipo, valor, outra parte) e não
 * um texto pronto: quem exibe decide a frase e o idioma. {@code eventId} identifica o fato que o originou. Junto com a
 * conta, garante que o mesmo evento nunca gera dois avisos.
 */
public record Notification(
        UUID id,
        UUID eventId,
        AccountId accountId,
        NotificationType type,
        Money amount,
        String counterpartName,
        Instant createdAt,
        Instant readAt) {

    public Notification {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(counterpartName, "counterpartName");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Notification unread(UUID eventId, AccountId accountId, NotificationType type, Money amount,
                                      String counterpartName, Instant createdAt) {
        return new Notification(UUID.randomUUID(), eventId, accountId, type, amount, counterpartName, createdAt, null);
    }

    public boolean isRead() {
        return readAt != null;
    }
}
