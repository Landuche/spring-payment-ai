package dio.budgeting.domain;

public enum Category {
    GROCERIES("Mercado"),
    PHARMA("Farmácia"),
    AUTO("Automóvel"),
    /** Qualquer gasto que não se encaixe nas demais: bar, adega, restaurante, lazer, etc. */
    OUTROS("Outros"),
    /** Usada apenas por lançamentos de transferência. */
    TRANSFERENCIA("Transferência");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    /** Nome exibido no extrato quando o usuário não informa uma descrição. */
    public String displayName() {
        return displayName;
    }
}
