package dio.budgeting.application.output;

import java.math.BigDecimal;

public record SimulationOutput(
        BigDecimal valorInvestido,
        BigDecimal montanteFinal,
        BigDecimal rendimento,
        int prazoMeses,
        BigDecimal taxaMensal) {
}
