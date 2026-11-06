package dio.budgeting.application;

import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Money;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultarSaldoUseCaseTest {

    @Test
    void should_computeBalance_asReceitasMinusDespesas_onlyForTheCurrentAccount() {
        var conta = new AccountId();
        var repository = new InMemoryTransactionRepository();
        var hoje = LocalDate.of(2026, 10, 1);
        repository.save(Transaction.nova(conta, "Salário", Money.ofReais(new BigDecimal("3000.00")), Category.AUTO, TransactionType.RECEITA, hoje));
        repository.save(Transaction.nova(conta, "Mercado", Money.ofReais(new BigDecimal("500.50")), Category.GROCERIES, TransactionType.DESPESA, hoje));
        repository.save(Transaction.nova(conta, "Farmácia", Money.ofReais(new BigDecimal("99.50")), Category.PHARMA, TransactionType.DESPESA, hoje));
        repository.save(Transaction.nova(new AccountId(), "Alheio", Money.ofReais(new BigDecimal("9999")), Category.AUTO, TransactionType.RECEITA, hoje));

        var saldo = new ConsultarSaldoUseCase(repository, () -> conta).execute();

        assertThat(saldo.receitas()).isEqualByComparingTo("3000.00");
        assertThat(saldo.despesas()).isEqualByComparingTo("600.00");
        assertThat(saldo.saldo()).isEqualByComparingTo("2400.00");
    }
}
