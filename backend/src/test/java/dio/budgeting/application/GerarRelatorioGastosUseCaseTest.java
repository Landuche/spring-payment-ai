package dio.budgeting.application;

import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.BusinessRuleViolationException;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Money;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GerarRelatorioGastosUseCaseTest {

    private final AccountId conta = new AccountId();
    private final InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
    private final GerarRelatorioGastosUseCase useCase = new GerarRelatorioGastosUseCase(repository, () -> conta);

    @Test
    void should_sumOnlyExpensesOfThePeriod_andFilterByCategory() {
        repository.save(despesa("Mercado", "100.00", Category.GROCERIES, LocalDate.of(2026, 10, 3)));
        repository.save(despesa("Farmácia", "40.00", Category.PHARMA, LocalDate.of(2026, 10, 9)));
        repository.save(despesa("Mercado antigo", "999.00", Category.GROCERIES, LocalDate.of(2026, 9, 30)));
        repository.save(Transaction.nova(conta, "Salário", Money.ofReais(new BigDecimal("5000")), Category.AUTO,
                TransactionType.RECEITA, LocalDate.of(2026, 10, 5)));

        var report = useCase.execute("2026-10", Category.GROCERIES);

        assertThat(report.total()).isEqualByComparingTo("100.00");
        assertThat(report.quantidade()).isEqualTo(1);
        assertThat(report.categoria()).isEqualTo("GROCERIES");
    }

    @Test
    void should_considerAllCategories_when_categoryIsNull() {
        repository.save(despesa("Mercado", "100.00", Category.GROCERIES, LocalDate.of(2026, 10, 3)));
        repository.save(despesa("Farmácia", "40.00", Category.PHARMA, LocalDate.of(2026, 10, 9)));

        var report = useCase.execute("2026-10", null);

        assertThat(report.total()).isEqualByComparingTo("140.00");
        assertThat(report.categoria()).isEqualTo("TODAS");
    }

    @Test
    void should_reportTransfersSeparately_withoutAddingThemToTheExpenseTotal() {
        var dia = LocalDate.of(2026, 10, 3);
        repository.save(despesa("Mercado", "100.00", Category.GROCERIES, dia));
        repository.save(transferencia("30.00", TransactionType.TRANSFERENCIA_ENVIADA, dia));
        repository.save(transferencia("20.00", TransactionType.TRANSFERENCIA_ENVIADA, dia));
        repository.save(transferencia("50.00", TransactionType.TRANSFERENCIA_RECEBIDA, dia));
        repository.save(transferencia("999.00", TransactionType.TRANSFERENCIA_ENVIADA, LocalDate.of(2026, 9, 30)));

        var report = useCase.execute("2026-10", null);

        assertThat(report.total()).isEqualByComparingTo("100.00");
        assertThat(report.quantidade()).isEqualTo(1);
        assertThat(report.transferenciasEnviadas()).isEqualByComparingTo("50.00");
        assertThat(report.transferenciasRecebidas()).isEqualByComparingTo("50.00");
    }

    @Test
    void should_reportZeroTransfers_when_thereAreNone() {
        repository.save(despesa("Mercado", "100.00", Category.GROCERIES, LocalDate.of(2026, 10, 3)));

        var report = useCase.execute("2026-10", null);

        assertThat(report.transferenciasEnviadas()).isEqualByComparingTo("0");
        assertThat(report.transferenciasRecebidas()).isEqualByComparingTo("0");
    }

    @Test
    void should_omitTransfers_when_reportIsFilteredByCategory() {
        var dia = LocalDate.of(2026, 10, 3);
        repository.save(despesa("Mercado", "100.00", Category.GROCERIES, dia));
        repository.save(transferencia("30.00", TransactionType.TRANSFERENCIA_ENVIADA, dia));

        var report = useCase.execute("2026-10", Category.GROCERIES);

        assertThat(report.transferenciasEnviadas()).isNull();
        assertThat(report.transferenciasRecebidas()).isNull();
    }

    @Test
    void should_rejectInvalidPeriod() {
        assertThatThrownBy(() -> useCase.execute("10/2026", null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    private Transaction transferencia(String valor, TransactionType tipo, LocalDate data) {
        return Transaction.deTransferencia(UUID.randomUUID(), conta, "Transferência",
                Money.ofReais(new BigDecimal(valor)), tipo, data);
    }

    private Transaction despesa(String descricao, String valor, Category categoria, LocalDate data) {
        return Transaction.nova(conta, descricao, Money.ofReais(new BigDecimal(valor)), categoria, TransactionType.DESPESA, data);
    }
}
