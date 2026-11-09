package dio.budgeting.transaction.domain;

/**
 * Tipo de lançamento. INCOME e EXPENSE são lançamentos manuais, e os de transferência
 * são gerados pelo caso de uso de transferência.
 */
public enum TransactionType {
    INCOME(true, false),
    EXPENSE(false, false),
    TRANSFER_RECEIVED(true, true),
    TRANSFER_SENT(false, true);

    private final boolean credit;
    private final boolean transfer;

    TransactionType(boolean credit, boolean transfer) {
        this.credit = credit;
        this.transfer = transfer;
    }

    /** Entrada de dinheiro na conta (soma ao saldo). */
    public boolean isCredit() {
        return credit;
    }

    public boolean isTransfer() {
        return transfer;
    }

    /** Despesa registrada pelo usuário: é a única que conta como gasto nos relatórios. */
    public boolean isExpense() {
        return this == EXPENSE;
    }
}
