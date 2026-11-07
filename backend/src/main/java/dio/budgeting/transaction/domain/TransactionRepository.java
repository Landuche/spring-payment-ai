package dio.budgeting.transaction.domain;

import dio.budgeting.account.domain.AccountId;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    List<Transaction> findAll(AccountId accountId);

    List<Transaction> findAllBetween(AccountId accountId, LocalDate start, LocalDate end);

    List<Transaction> findAllBetween(AccountId accountId, LocalDate start, LocalDate end, Category category);
}
