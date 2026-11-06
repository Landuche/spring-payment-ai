package dio.budgeting.investment.application;

import java.math.BigDecimal;

public record SimulationOutput(
        BigDecimal valorInvestido,
        BigDecimal montanteFinal,
        BigDecimal rendimento,
        int prazoMeses,
        BigDecimal taxaMensal) {
}
