package dio.budgeting.application;

import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Money;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ListarTransacoesUseCaseTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 4);

    private final AccountId conta = new AccountId();
    private final InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
    private final ListarTransacoesUseCase useCase = new ListarTransacoesUseCase(repository, () -> conta);

    private void salvar(String descricao, LocalDate dia, Instant momento) {
        repository.save(Transaction.nova(conta, descricao, new Money(1000), Category.GROCERIES,
                TransactionType.DESPESA, dia, momento));
    }

    @Test
    void should_listNewestDayFirst() {
        salvar("antiga", HOJE.minusDays(2), null);
        salvar("recente", HOJE, null);
        salvar("meio", HOJE.minusDays(1), null);

        assertThat(useCase.execute()).extracting("description").containsExactly("recente", "meio", "antiga");
    }

    @Test
    void should_listLatestMomentFirst_withinTheSameDay() {
        salvar("manhã", HOJE, Instant.parse("2026-11-04T12:00:00Z"));
        salvar("noite", HOJE, Instant.parse("2026-11-04T23:00:00Z"));
        salvar("tarde", HOJE, Instant.parse("2026-11-04T18:00:00Z"));

        assertThat(useCase.execute()).extracting("description").containsExactly("noite", "tarde", "manhã");
    }

    @Test
    void should_putEntriesWithoutMomentAfterTheOnesWithMoment_withinTheSameDay() {
        salvar("sem hora", HOJE, null);
        salvar("com hora", HOJE, Instant.parse("2026-11-04T12:00:00Z"));

        assertThat(useCase.execute()).extracting("description").containsExactly("com hora", "sem hora");
    }
}
