package dio.budgeting.investment.application;

import java.math.BigDecimal;

public record SimulationOutput(
        BigDecimal investedAmount,
        BigDecimal finalAmount,
        BigDecimal earnings,
        int termMonths,
        BigDecimal monthlyRate) {
}
