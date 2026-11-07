package dio.budgeting.transaction.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionType;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerateExpenseReportUseCaseTest {

    private final AccountId account = new AccountId();
    private final InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
    private final GenerateExpenseReportUseCase useCase = new GenerateExpenseReportUseCase(repository, () -> account);

    @Test
    void should_sumOnlyExpensesOfThePeriod_andFilterByCategory() {
        repository.save(expense("Mercado", "100.00", Category.GROCERIES, LocalDate.of(2026, 10, 3)));
        repository.save(expense("Farmácia", "40.00", Category.PHARMA, LocalDate.of(2026, 10, 9)));
        repository.save(expense("Mercado antigo", "999.00", Category.GROCERIES, LocalDate.of(2026, 9, 30)));
        repository.save(Transaction.create(account, "Salário", Money.ofReais(new BigDecimal("5000")), Category.AUTO,
                TransactionType.INCOME, LocalDate.of(2026, 10, 5)));

        var report = useCase.execute("2026-10", Category.GROCERIES);

        assertThat(report.total()).isEqualByComparingTo("100.00");
        assertThat(report.count()).isEqualTo(1);
        assertThat(report.category()).isEqualTo("GROCERIES");
    }

    @Test
    void should_considerAllCategories_when_categoryIsNull() {
        repository.save(expense("Mercado", "100.00", Category.GROCERIES, LocalDate.of(2026, 10, 3)));
        repository.save(expense("Farmácia", "40.00", Category.PHARMA, LocalDate.of(2026, 10, 9)));

        var report = useCase.execute("2026-10", null);

        assertThat(report.total()).isEqualByComparingTo("140.00");
        assertThat(report.category()).isEqualTo("ALL");
    }

    @Test
    void should_reportTransfersSeparately_withoutAddingThemToTheExpenseTotal() {
        var day = LocalDate.of(2026, 10, 3);
        repository.save(expense("Mercado", "100.00", Category.GROCERIES, day));
        repository.save(transfer("30.00", TransactionType.TRANSFER_SENT, day));
        repository.save(transfer("20.00", TransactionType.TRANSFER_SENT, day));
        repository.save(transfer("50.00", TransactionType.TRANSFER_RECEIVED, day));
        repository.save(transfer("999.00", TransactionType.TRANSFER_SENT, LocalDate.of(2026, 9, 30)));

        var report = useCase.execute("2026-10", null);

        assertThat(report.total()).isEqualByComparingTo("100.00");
        assertThat(report.count()).isEqualTo(1);
        assertThat(report.sentTransfers()).isEqualByComparingTo("50.00");
        assertThat(report.receivedTransfers()).isEqualByComparingTo("50.00");
    }

    @Test
    void should_reportZeroTransfers_when_thereAreNone() {
        repository.save(expense("Mercado", "100.00", Category.GROCERIES, LocalDate.of(2026, 10, 3)));

        var report = useCase.execute("2026-10", null);

        assertThat(report.sentTransfers()).isEqualByComparingTo("0");
        assertThat(report.receivedTransfers()).isEqualByComparingTo("0");
    }

    @Test
    void should_omitTransfers_when_reportIsFilteredByCategory() {
        var day = LocalDate.of(2026, 10, 3);
        repository.save(expense("Mercado", "100.00", Category.GROCERIES, day));
        repository.save(transfer("30.00", TransactionType.TRANSFER_SENT, day));

        var report = useCase.execute("2026-10", Category.GROCERIES);

        assertThat(report.sentTransfers()).isNull();
        assertThat(report.receivedTransfers()).isNull();
    }

    @Test
    void should_rejectInvalidPeriod() {
        assertThatThrownBy(() -> useCase.execute("10/2026", null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    private Transaction transfer(String amount, TransactionType type, LocalDate date) {
        return Transaction.fromTransfer(UUID.randomUUID(), account, "Transferência",
                Money.ofReais(new BigDecimal(amount)), type, date);
    }

    private Transaction expense(String description, String amount, Category category, LocalDate date) {
        return Transaction.create(account, description, Money.ofReais(new BigDecimal(amount)), category, TransactionType.EXPENSE, date);
    }
}
