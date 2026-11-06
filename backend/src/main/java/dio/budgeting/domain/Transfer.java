package dio.budgeting.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Transferência entre duas contas. Os lançamentos de débito e crédito são criados junto com ela. */
public record Transfer(
        UUID id,
        AccountId origin,
        AccountId destination,
        Money amount,
        String description,
        String idempotencyKey,
        Instant createdAt) {

    static final int MAX_IDEMPOTENCY_KEY = 64;

    public Transfer {
        Objects.requireNonNull(id, "O identificador é obrigatório");
        Objects.requireNonNull(origin, "A conta de origem é obrigatória");
        Objects.requireNonNull(destination, "A conta de destino é obrigatória");
        Objects.requireNonNull(createdAt, "A data é obrigatória");
        if (origin.equals(destination)) {
            throw new BusinessRuleViolationException("Não é possível transferir para a própria conta");
        }
        if (amount == null || amount.cents() <= 0) {
            throw new BusinessRuleViolationException("O valor da transferência deve ser maior que zero");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY) {
            throw new BusinessRuleViolationException("Idempotency-Key obrigatória (até 64 caracteres)");
        }
        // Descrição é opcional: fica null quando não informada e o caso de uso aplica o texto padrão.
        description = description == null || description.isBlank() ? null : description.strip();
        if (description != null && description.length() > Transaction.MAX_DESCRIPTION_LENGTH) {
            throw new BusinessRuleViolationException("A descrição da transferência é muito longa");
        }
    }
}
