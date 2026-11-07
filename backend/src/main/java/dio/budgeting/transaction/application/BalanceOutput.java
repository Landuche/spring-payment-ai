package dio.budgeting.transaction.application;

import java.math.BigDecimal;

public record BalanceOutput(BigDecimal income, BigDecimal expenses, BigDecimal balance) {
}
