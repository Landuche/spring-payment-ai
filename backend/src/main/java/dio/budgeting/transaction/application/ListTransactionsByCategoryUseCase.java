package dio.budgeting.transaction.application;

import dio.budgeting.account.application.CurrentAccountProvider;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.TransactionRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListTransactionsByCategoryUseCase {
    private final TransactionRepository transactionRepository;
    private final CurrentAccountProvider currentAccount;

    public ListTransactionsByCategoryUseCase(TransactionRepository transactionRepository,
                                               CurrentAccountProvider currentAccount) {
        this.transactionRepository = transactionRepository;
        this.currentAccount = currentAccount;
    }

    public List<TransactionOutput> execute(Category category) {
        return transactionRepository.findAll(currentAccount.current()).stream()
                .filter(t -> t.category() == category)
                .map(TransactionOutput::from)
                .toList();
    }
}
