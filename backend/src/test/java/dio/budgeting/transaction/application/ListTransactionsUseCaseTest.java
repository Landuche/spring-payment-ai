package dio.budgeting.transaction.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionType;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ListTransactionsUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    private final AccountId account = new AccountId();
    private final InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
    private final ListTransactionsUseCase useCase = new ListTransactionsUseCase(repository, () -> account);

    private void store(String description, LocalDate day, Instant moment) {
        repository.save(Transaction.create(account, description, new Money(1000), Category.GROCERIES,
                TransactionType.EXPENSE, day, moment));
    }

    @Test
    void should_listNewestDayFirst() {
        store("antiga", TODAY.minusDays(2), null);
        store("recente", TODAY, null);
        store("meio", TODAY.minusDays(1), null);

        assertThat(useCase.execute()).extracting("description").containsExactly("recente", "meio", "antiga");
    }

    @Test
    void should_listLatestMomentFirst_withinTheSameDay() {
        store("manhã", TODAY, Instant.parse("2026-11-04T12:00:00Z"));
        store("noite", TODAY, Instant.parse("2026-11-04T23:00:00Z"));
        store("tarde", TODAY, Instant.parse("2026-11-04T18:00:00Z"));

        assertThat(useCase.execute()).extracting("description").containsExactly("noite", "tarde", "manhã");
    }

    @Test
    void should_putEntriesWithoutMomentAfterTheOnesWithMoment_withinTheSameDay() {
        store("sem hora", TODAY, null);
        store("com hora", TODAY, Instant.parse("2026-11-04T12:00:00Z"));

        assertThat(useCase.execute()).extracting("description").containsExactly("com hora", "sem hora");
    }
}
