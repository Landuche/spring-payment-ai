package dio.budgeting.application;

import dio.budgeting.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimularInvestimentoUseCaseTest {

    private final SimularInvestimentoUseCase useCase = new SimularInvestimentoUseCase(new BigDecimal("0.01"));

    @Test
    void should_applyMonthlyCompoundInterest() {
        // 1000 * 1.01^12 = 1126.825... -> 1126.83
        var result = useCase.execute(new BigDecimal("1000"), 12);

        assertThat(result.montanteFinal()).isEqualByComparingTo("1126.83");
        assertThat(result.rendimento()).isEqualByComparingTo("126.83");
        assertThat(result.prazoMeses()).isEqualTo(12);
    }

    @Test
    void should_rejectPrazoOutOfRange() {
        assertThatThrownBy(() -> useCase.execute(new BigDecimal("1000"), 0))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> useCase.execute(new BigDecimal("1000"), 361))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void should_rejectNonPositiveValor() {
        assertThatThrownBy(() -> useCase.execute(BigDecimal.ZERO, 6))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
