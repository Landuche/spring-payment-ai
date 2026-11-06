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

class CriarTransacaoUseCaseTest {

    private final AccountId conta = new AccountId();
    private final InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);
    private final CriarTransacaoUseCase useCase =
            new CriarTransacaoUseCase(repository, accounts, () -> conta, clock);

    @Test
    void should_persistTransaction_withTodayDate_inTheCurrentAccount() {
        seedReceita("100.00");

        var output = useCase.execute(new BigDecimal("45.90"), Category.GROCERIES, TransactionType.DESPESA, "Mercado");

        assertThat(output.amount()).isEqualByComparingTo("45.90");
        assertThat(output.occurredAt()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(repository.findAll(conta)).hasSize(2);
    }

    @Test
    void should_rejectExpense_whenBalanceIsInsufficient_withoutPersisting() {
        seedReceita("50.00");

        assertThatThrownBy(() -> useCase.execute(new BigDecimal("50.01"), Category.AUTO, TransactionType.DESPESA, "Posto"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Saldo insuficiente");
        assertThat(repository.findAll(conta)).hasSize(1);
    }

    @Test
    void should_allowExpense_whenItExactlyConsumesBalance() {
        seedReceita("50.00");

        useCase.execute(new BigDecimal("50.00"), Category.AUTO, TransactionType.DESPESA, "Posto");

        assertThat(repository.findAll(conta)).hasSize(2);
    }

    @Test
    void should_rejectExpense_whenAccountIsEmpty() {
        assertThatThrownBy(() -> useCase.execute(new BigDecimal("0.01"), Category.PHARMA, TransactionType.DESPESA, "Remédio"))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(repository.findAll(conta)).isEmpty();
    }

    @Test
    void should_acceptIncome_regardlessOfBalance() {
        useCase.execute(new BigDecimal("300.00"), Category.AUTO, TransactionType.RECEITA, "Salário");

        assertThat(repository.findAll(conta)).hasSize(1);
    }

    @Test
    void should_lockAccount_beforeCheckingBalance_forExpenses() {
        seedReceita("100.00");
        var locksBefore = accounts.locks();

        useCase.execute(new BigDecimal("10.00"), Category.GROCERIES, TransactionType.DESPESA, "Mercado");

        assertThat(accounts.locks()).isEqualTo(locksBefore + 1);
    }

    @Test
    void should_neverSeeAnotherAccountsBalance() {
        var outraConta = new AccountId();
        var outro = new CriarTransacaoUseCase(repository, accounts, () -> outraConta, clock);
        outro.execute(new BigDecimal("1000.00"), Category.AUTO, TransactionType.RECEITA, "Salário do outro");

        assertThatThrownBy(() -> useCase.execute(new BigDecimal("1.00"), Category.PHARMA, TransactionType.DESPESA, "Remédio"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    private void seedReceita(String valor) {
        useCase.execute(new BigDecimal(valor), Category.AUTO, TransactionType.RECEITA, "Saldo inicial");
    }
}
