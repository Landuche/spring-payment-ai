package dio.budgeting.transaction.application;

import dio.budgeting.account.application.InMemoryAccountRepository;
import dio.budgeting.account.domain.AccountId;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.TransactionType;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateTransactionUseCaseTest {

    private final AccountId account = new AccountId();
    private final InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);
    private final CreateTransactionUseCase useCase =
            new CreateTransactionUseCase(repository, accounts, () -> account, clock);

    @Test
    void should_persistTransaction_withTodayDate_inTheCurrentAccount() {
        seedReceita("100.00");

        var output = useCase.execute(new BigDecimal("45.90"), Category.GROCERIES, TransactionType.EXPENSE, "Mercado");

        assertThat(output.amount()).isEqualByComparingTo("45.90");
        assertThat(output.occurredAt()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(repository.findAll(account)).hasSize(2);
    }

    @Test
    void should_rejectExpense_whenBalanceIsInsufficient_withoutPersisting() {
        seedReceita("50.00");

        assertThatThrownBy(() -> useCase.execute(new BigDecimal("50.01"), Category.AUTO, TransactionType.EXPENSE, "Posto"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Saldo insuficiente");
        assertThat(repository.findAll(account)).hasSize(1);
    }

    @Test
    void should_allowExpense_whenItExactlyConsumesBalance() {
        seedReceita("50.00");

        useCase.execute(new BigDecimal("50.00"), Category.AUTO, TransactionType.EXPENSE, "Posto");

        assertThat(repository.findAll(account)).hasSize(2);
    }

    @Test
    void should_rejectExpense_whenAccountIsEmpty() {
        assertThatThrownBy(() -> useCase.execute(new BigDecimal("0.01"), Category.PHARMA, TransactionType.EXPENSE, "Remédio"))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(repository.findAll(account)).isEmpty();
    }

    @Test
    void should_acceptIncome_regardlessOfBalance() {
        useCase.execute(new BigDecimal("300.00"), Category.AUTO, TransactionType.INCOME, "Salário");

        assertThat(repository.findAll(account)).hasSize(1);
    }

    @Test
    void should_lockAccount_beforeCheckingBalance_forExpenses() {
        seedReceita("100.00");
        var locksBefore = accounts.locks();

        useCase.execute(new BigDecimal("10.00"), Category.GROCERIES, TransactionType.EXPENSE, "Mercado");

        assertThat(accounts.locks()).isEqualTo(locksBefore + 1);
    }

    @Test
    void should_neverSeeAnotherAccountsBalance() {
        var otherAccount = new AccountId();
        var other = new CreateTransactionUseCase(repository, accounts, () -> otherAccount, clock);
        other.execute(new BigDecimal("1000.00"), Category.AUTO, TransactionType.INCOME, "Salário do outro");

        assertThatThrownBy(() -> useCase.execute(new BigDecimal("1.00"), Category.PHARMA, TransactionType.EXPENSE, "Remédio"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    private void seedReceita(String amount) {
        useCase.execute(new BigDecimal(amount), Category.AUTO, TransactionType.INCOME, "Saldo inicial");
    }
}
