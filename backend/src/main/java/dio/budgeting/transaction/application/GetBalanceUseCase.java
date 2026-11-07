package dio.budgeting.transaction.application;

import dio.budgeting.account.application.CurrentAccountProvider;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetBalanceUseCase {
    private final TransactionRepository transactionRepository;
    private final CurrentAccountProvider currentAccount;

    public GetBalanceUseCase(TransactionRepository transactionRepository, CurrentAccountProvider currentAccount) {
        this.transactionRepository = transactionRepository;
        this.currentAccount = currentAccount;
    }

    public BalanceOutput execute() {
        List<Transaction> transactions = transactionRepository.findAll(currentAccount.current());

        Money credits = sum(transactions, true);
        Money debits = sum(transactions, false);

        return new BalanceOutput(credits.toReais(), debits.toReais(), credits.minus(debits).toReais());
    }

    private static Money sum(List<Transaction> transactions, boolean credit) {
        return transactions.stream()
                .filter(t -> t.type().isCredit() == credit)
                .map(Transaction::amount)
                .reduce(Money.ZERO, Money::plus);
    }
}
