package dio.budgeting.transaction.application;

import dio.budgeting.account.application.CurrentAccountProvider;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionRepository;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Extrato da conta do usuário logado, da mais recente para a mais antiga. Dentro do mesmo dia, a hora desempata;
 * lançamentos sem hora ficam depois dos que têm.
 */
@Service
public class ListarTransacoesUseCase {
    private static final Comparator<Transaction> DA_MAIS_ANTIGA_PARA_A_MAIS_RECENTE =
            Comparator.comparing(Transaction::occurredAt)
                    .thenComparing(Transaction::createdAt, Comparator.nullsFirst(Comparator.naturalOrder()));

    private final TransactionRepository transactionRepository;
    private final CurrentAccountProvider currentAccount;

    public ListarTransacoesUseCase(TransactionRepository transactionRepository, CurrentAccountProvider currentAccount) {
        this.transactionRepository = transactionRepository;
        this.currentAccount = currentAccount;
    }

    public List<TransactionOutput> execute() {
        return transactionRepository.findAll(currentAccount.current()).stream()
                .sorted(DA_MAIS_ANTIGA_PARA_A_MAIS_RECENTE.reversed())
                .map(TransactionOutput::from)
                .toList();
    }
}
