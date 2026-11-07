package dio.budgeting.transaction.domain;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Lançamento imutável em uma conta. Invariantes garantidas no construtor.
 * {@code transferId} só é preenchido nos lançamentos gerados por uma transferência.
 * {@code createdAt} é o momento exato; é opcional (nulo quando só a data é conhecida).
 */
public record Transaction(
        TransactionId id,
        AccountId accountId,
        String description,
        Money amount,
        Category category,
        TransactionType type,
        LocalDate occurredAt,
        Instant createdAt,
        UUID transferId) {

    public static final int MAX_DESCRIPTION_LENGTH = 120;

    public Transaction {
        Objects.requireNonNull(id, "O identificador é obrigatório");
        Objects.requireNonNull(accountId, "A conta é obrigatória");
        Objects.requireNonNull(category, "A categoria é obrigatória");
        Objects.requireNonNull(type, "O tipo é obrigatório");
        Objects.requireNonNull(occurredAt, "A data é obrigatória");

        if (type.isTransfer() != (transferId != null)) {
            throw new BusinessRuleViolationException("Lançamento de transferência exige a transferência de origem");
        }
        if (amount == null || amount.cents() <= 0) {
            throw new BusinessRuleViolationException("O valor deve ser maior que zero");
        }
        // Descrição é opcional: sem texto, o extrato usa o nome da categoria.
        description = description == null || description.isBlank() ? category.displayName() : description.strip();
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessRuleViolationException(
                    "A descrição deve ter no máximo " + MAX_DESCRIPTION_LENGTH + " caracteres");
        }
    }

    /** Valor com sinal: positivo para entradas, negativo para saídas. */
    public Money signedAmount() {
        return type.isCredit() ? amount : new Money(-amount.cents());
    }

    /** Lançamento manual (receita ou despesa). Tipos de transferência só são criados pelo caso de uso de transferência. */
    public static Transaction create(AccountId accountId, String description, Money amount, Category category,
                                     TransactionType type, LocalDate occurredAt) {
        return create(accountId, description, amount, category, type, occurredAt, null);
    }

    public static Transaction create(AccountId accountId, String description, Money amount, Category category,
                                     TransactionType type, LocalDate occurredAt, Instant createdAt) {
        if (type.isTransfer()) {
            throw new BusinessRuleViolationException("Use a operação de transferência para movimentar entre contas");
        }
        return new Transaction(new TransactionId(), accountId, description, amount, category, type, occurredAt,
                createdAt, null);
    }

    public static Transaction fromTransfer(UUID transferId, AccountId accountId, String description, Money amount,
                                           TransactionType type, LocalDate occurredAt) {
        return fromTransfer(transferId, accountId, description, amount, type, occurredAt, null);
    }

    public static Transaction fromTransfer(UUID transferId, AccountId accountId, String description, Money amount,
                                           TransactionType type, LocalDate occurredAt, Instant createdAt) {
        return new Transaction(new TransactionId(), accountId, description, amount,
                Category.TRANSFER, type, occurredAt, createdAt, transferId);
    }
}
