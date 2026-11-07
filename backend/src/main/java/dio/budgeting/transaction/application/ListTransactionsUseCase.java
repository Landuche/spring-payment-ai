package dio.budgeting.transaction.application;

import dio.budgeting.account.application.CurrentAccountProvider;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionRepository;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ListTransactionsUseCase {
    private static final Comparator<Transaction> NEWEST_FIRST =
            Comparator.comparing(Transaction::occurredAt)
                    .thenComparing(Transaction::createdAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .reversed();

    private final TransactionRepository transactionRepository;
    private final CurrentAccountProvider currentAccount;

    public ListTransactionsUseCase(TransactionRepository transactionRepository, CurrentAccountProvider currentAccount) {
        this.transactionRepository = transactionRepository;
        this.currentAccount = currentAccount;
    }

    public List<TransactionOutput> execute() {
        return transactionRepository.findAll(currentAccount.current()).stream()
                .sorted(NEWEST_FIRST)
                .map(TransactionOutput::from)
                .toList();
    }
}
