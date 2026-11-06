package dio.budgeting.application.output;

import java.math.BigDecimal;

public record BalanceOutput(BigDecimal receitas, BigDecimal despesas, BigDecimal saldo) {
}
