package dio.budgeting.transaction.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionType;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GetBalanceUseCaseTest {

    @Test
    void should_computeBalance_asReceitasMinusDespesas_onlyForTheCurrentAccount() {
        var account = new AccountId();
        var repository = new InMemoryTransactionRepository();
        var today = LocalDate.of(2026, 10, 1);
        repository.save(Transaction.create(account, "Salário", Money.ofReais(new BigDecimal("3000.00")), Category.AUTO, TransactionType.INCOME, today));
        repository.save(Transaction.create(account, "Mercado", Money.ofReais(new BigDecimal("500.50")), Category.GROCERIES, TransactionType.EXPENSE, today));
        repository.save(Transaction.create(account, "Farmácia", Money.ofReais(new BigDecimal("99.50")), Category.PHARMA, TransactionType.EXPENSE, today));
        repository.save(Transaction.create(new AccountId(), "Alheio", Money.ofReais(new BigDecimal("9999")), Category.AUTO, TransactionType.INCOME, today));

        var balance = new GetBalanceUseCase(repository, () -> account).execute();

        assertThat(balance.income()).isEqualByComparingTo("3000.00");
        assertThat(balance.expenses()).isEqualByComparingTo("600.00");
        assertThat(balance.balance()).isEqualByComparingTo("2400.00");
    }
}
