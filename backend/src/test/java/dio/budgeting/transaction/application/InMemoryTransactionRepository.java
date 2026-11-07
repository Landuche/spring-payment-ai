package dio.budgeting.transaction.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Implementação em memória do contrato do domínio, sempre filtrada por conta. */
public class InMemoryTransactionRepository implements TransactionRepository {
    private final List<Transaction> store = new ArrayList<>();

    @Override
    public Transaction save(Transaction transaction) {
        store.add(transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findAll(AccountId accountId) {
        return store.stream().filter(t -> t.accountId().equals(accountId)).toList();
    }

    @Override
    public List<Transaction> findAllBetween(AccountId accountId, LocalDate start, LocalDate end) {
        return findAll(accountId).stream()
                .filter(t -> !t.occurredAt().isBefore(start) && !t.occurredAt().isAfter(end))
                .toList();
    }

    @Override
    public List<Transaction> findAllBetween(AccountId accountId, LocalDate start, LocalDate end, Category category) {
        return findAllBetween(accountId, start, end).stream()
                .filter(t -> t.category() == category)
                .toList();
    }
}
