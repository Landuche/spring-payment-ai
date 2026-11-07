package dio.budgeting.transaction.domain;

public enum Category {
    GROCERIES("Mercado"),
    PHARMA("Farmácia"),
    AUTO("Automóvel"),
    OTHER("Outros"),
    TRANSFER("Transferência");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
