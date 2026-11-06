package dio.budgeting.transaction.domain;

/**
 * Tipo de lançamento. RECEITA e DESPESA são lançamentos manuais; os de transferência
 * são gerados pelo caso de uso de transferência e nunca pela criação manual.
 */
public enum TransactionType {
    RECEITA(true, false),
    DESPESA(false, false),
    TRANSFERENCIA_RECEBIDA(true, true),
    TRANSFERENCIA_ENVIADA(false, true);

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
        return this == DESPESA;
    }
}
