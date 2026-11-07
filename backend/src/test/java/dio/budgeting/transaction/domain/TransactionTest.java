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

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final AccountId ACCOUNT = new AccountId();

    @Test
    void should_trimDescription_when_valid() {
        var t = Transaction.create(ACCOUNT, "  Mercado  ", new Money(1000), Category.GROCERIES, TransactionType.EXPENSE, TODAY);
        assertThat(t.description()).isEqualTo("Mercado");
    }

    @Test
    void should_keepExactMoment_when_informed() {
        var now = Instant.parse("2026-10-15T17:23:00Z");
        var t = Transaction.create(ACCOUNT, "Mercado", new Money(1000), Category.GROCERIES, TransactionType.EXPENSE, TODAY, now);
        assertThat(t.createdAt()).isEqualTo(now);
    }

    @Test
    void should_haveNoMoment_when_onlyDateIsKnown() {
        var t = Transaction.create(ACCOUNT, "Mercado", new Money(1000), Category.GROCERIES, TransactionType.EXPENSE, TODAY);
        assertThat(t.createdAt()).isNull();
    }

    @Test
    void should_rejectZeroAmount() {
        assertThatThrownBy(() -> Transaction.create(ACCOUNT, "Café", new Money(0), Category.GROCERIES, TransactionType.EXPENSE, TODAY))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void should_useCategoryName_whenDescriptionIsBlank() {
        var t = Transaction.create(ACCOUNT, "   ", new Money(100), Category.PHARMA, TransactionType.EXPENSE, TODAY);
        assertThat(t.description()).isEqualTo("Farmácia");
    }

    @Test
    void should_rejectTooLongDescription() {
        var longDescription = "x".repeat(Transaction.MAX_DESCRIPTION_LENGTH + 1);
        assertThatThrownBy(() -> Transaction.create(ACCOUNT, longDescription, new Money(100), Category.AUTO, TransactionType.EXPENSE, TODAY))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void should_requireAccount() {
        assertThatThrownBy(() -> Transaction.create(null, "Café", new Money(100), Category.AUTO, TransactionType.EXPENSE, TODAY))
                .isInstanceOf(NullPointerException.class);
    }
}
