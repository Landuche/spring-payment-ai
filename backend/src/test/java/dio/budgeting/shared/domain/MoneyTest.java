package dio.budgeting.shared.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void should_convertReaisToCents_withHalfUpRounding() {
        assertThat(Money.ofReais(new BigDecimal("45.905")).cents()).isEqualTo(4591);
        assertThat(Money.ofReais(new BigDecimal("10")).cents()).isEqualTo(1000);
    }

    @Test
    void should_rejectNonPositiveAmount_when_positiveRequired() {
        assertThatThrownBy(() -> Money.ofPositiveReais(BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> Money.ofPositiveReais(new BigDecimal("-1")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void should_keepExactPrecision_when_summing() {
        var total = Money.ofReais(new BigDecimal("0.10")).plus(Money.ofReais(new BigDecimal("0.20")));
        assertThat(total.toReais()).isEqualByComparingTo("0.30");
    }
}
