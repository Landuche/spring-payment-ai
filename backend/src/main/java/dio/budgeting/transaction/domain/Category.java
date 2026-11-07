package dio.budgeting.transaction.domain;

public enum Category {
    GROCERIES("Mercado"),
    PHARMA("Farmácia"),
    AUTO("Automóvel"),
    /** Qualquer gasto que não se encaixe nas demais: bar, adega, restaurante, lazer, etc. */
    OTHER("Outros"),
    /** Usada apenas por lançamentos de transferência. */
    TRANSFER("Transferência");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    /** Nome exibido no extrato quando o usuário não informa uma descrição. */
    public String displayName() {
        return displayName;
    }
}
