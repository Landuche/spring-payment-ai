package dio.budgeting.transaction.domain;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.Money;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 15);
    private static final AccountId CONTA = new AccountId();

    @Test
    void should_trimDescription_when_valid() {
        var t = Transaction.nova(CONTA, "  Mercado  ", new Money(1000), Category.GROCERIES, TransactionType.DESPESA, HOJE);
        assertThat(t.description()).isEqualTo("Mercado");
    }

    @Test
    void should_keepExactMoment_when_informed() {
        var agora = Instant.parse("2026-10-15T17:23:00Z");
        var t = Transaction.nova(CONTA, "Mercado", new Money(1000), Category.GROCERIES, TransactionType.DESPESA, HOJE, agora);
        assertThat(t.createdAt()).isEqualTo(agora);
    }

    @Test
    void should_haveNoMoment_when_onlyDateIsKnown() {
        var t = Transaction.nova(CONTA, "Mercado", new Money(1000), Category.GROCERIES, TransactionType.DESPESA, HOJE);
        assertThat(t.createdAt()).isNull();
    }

    @Test
    void should_rejectZeroAmount() {
        assertThatThrownBy(() -> Transaction.nova(CONTA, "Café", new Money(0), Category.GROCERIES, TransactionType.DESPESA, HOJE))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void should_useCategoryName_whenDescriptionIsBlank() {
        var t = Transaction.nova(CONTA, "   ", new Money(100), Category.PHARMA, TransactionType.DESPESA, HOJE);
        assertThat(t.description()).isEqualTo("Farmácia");
    }

    @Test
    void should_rejectTooLongDescription() {
        var longa = "x".repeat(Transaction.MAX_DESCRIPTION_LENGTH + 1);
        assertThatThrownBy(() -> Transaction.nova(CONTA, longa, new Money(100), Category.AUTO, TransactionType.DESPESA, HOJE))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void should_requireAccount() {
        assertThatThrownBy(() -> Transaction.nova(null, "Café", new Money(100), Category.AUTO, TransactionType.DESPESA, HOJE))
                .isInstanceOf(NullPointerException.class);
    }
}
