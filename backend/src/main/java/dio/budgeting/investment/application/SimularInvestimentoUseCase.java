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
public class SimularInvestimentoUseCase {
    static final int MAX_PRAZO_MESES = 360;

    private final BigDecimal taxaMensal;

    public SimularInvestimentoUseCase(
            @Value("${budgeting.investment.monthly-rate:0.008}") BigDecimal taxaMensal) {
        if (taxaMensal == null || taxaMensal.signum() < 0) {
            throw new IllegalStateException("A taxa mensal de investimento não pode ser negativa");
        }
        this.taxaMensal = taxaMensal;
    }

    public SimulationOutput execute(BigDecimal valor, int prazo) {

        if (prazo < 1 || prazo > MAX_PRAZO_MESES) {
            throw new BusinessRuleViolationException(
                    "O prazo deve estar entre 1 e " + MAX_PRAZO_MESES + " meses");
        }

        var principal = Money.ofPositiveReais(valor).toReais();
        var fator = BigDecimal.ONE.add(taxaMensal).pow(prazo, MathContext.DECIMAL128);
        var montanteFinal = principal.multiply(fator).setScale(2, RoundingMode.HALF_UP);

        return new SimulationOutput(
                principal,
                montanteFinal,
                montanteFinal.subtract(principal),
                prazo,
                taxaMensal);
    }
}
