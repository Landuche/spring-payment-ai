package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.application.port.CurrentAccountProvider;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarTransacoesPorCategoriaUseCase {
    private final TransactionRepository transactionRepository;
    private final CurrentAccountProvider currentAccount;

    public ListarTransacoesPorCategoriaUseCase(TransactionRepository transactionRepository,
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
