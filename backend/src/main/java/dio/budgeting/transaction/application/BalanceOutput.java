package dio.budgeting.transaction.application;

import java.math.BigDecimal;

public record BalanceOutput(BigDecimal receitas, BigDecimal despesas, BigDecimal saldo) {
}
