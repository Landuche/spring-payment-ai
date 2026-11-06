package dio.budgeting.domain;

import java.time.LocalDate;
import java.util.List;

/** Todas as consultas são sempre restritas a uma conta: não existe leitura global. */
public interface TransactionRepository {
    Transaction save(Transaction transaction);

    List<Transaction> findAll(AccountId accountId);

    List<Transaction> findAllBetween(AccountId accountId, LocalDate start, LocalDate end);

    List<Transaction> findAllBetween(AccountId accountId, LocalDate start, LocalDate end, Category category);
}
