package dio.budgeting.investment.application;

import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.Money;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Simula investimento com juros compostos mensais.
 * A taxa vem da configuração (budgeting.investment.monthly-rate), padrão 0.8% ao mês.
 */
@Service
public class SimulateInvestmentUseCase {
    static final int MAX_TERM_MONTHS = 360;

    private final BigDecimal monthlyRate;

    public SimulateInvestmentUseCase(
            @Value("${budgeting.investment.monthly-rate:0.008}") BigDecimal monthlyRate) {
        if (monthlyRate == null || monthlyRate.signum() < 0) {
            throw new IllegalStateException("A taxa mensal de investimento não pode ser negativa");
        }
        this.monthlyRate = monthlyRate;
    }

    public SimulationOutput execute(BigDecimal amount, int termMonths) {

        if (termMonths < 1 || termMonths > MAX_TERM_MONTHS) {
            throw new BusinessRuleViolationException(
                    "O prazo deve estar entre 1 e " + MAX_TERM_MONTHS + " meses");
        }

        var principal = Money.ofPositiveReais(amount).toReais();
        var factor = BigDecimal.ONE.add(monthlyRate).pow(termMonths, MathContext.DECIMAL128);
        var finalAmount = principal.multiply(factor).setScale(2, RoundingMode.HALF_UP);

        return new SimulationOutput(
                principal,
                finalAmount,
                finalAmount.subtract(principal),
                termMonths,
                monthlyRate);
    }
}
