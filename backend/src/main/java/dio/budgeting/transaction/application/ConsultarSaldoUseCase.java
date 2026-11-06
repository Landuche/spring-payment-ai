package dio.budgeting.transaction.application;

import dio.budgeting.account.application.CurrentAccountProvider;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionRepository;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsultarSaldoUseCase {
    private final TransactionRepository transactionRepository;
    private final CurrentAccountProvider currentAccount;

    public ConsultarSaldoUseCase(TransactionRepository transactionRepository, CurrentAccountProvider currentAccount) {
        this.transactionRepository = transactionRepository;
        this.currentAccount = currentAccount;
    }

    @Tool(name = "consultarSaldo",
            description = "Consulta o saldo atual da conta. O resultado traz também receitas e despesas, mas ao "
                    + "responder informe apenas o saldo, a menos que o cliente peça as entradas e saídas")
    public BalanceOutput execute() {
        List<Transaction> transactions = transactionRepository.findAll(currentAccount.current());

        Money entradas = sum(transactions, true);
        Money saidas = sum(transactions, false);

        return new BalanceOutput(entradas.toReais(), saidas.toReais(), entradas.minus(saidas).toReais());
    }

    private static Money sum(List<Transaction> transactions, boolean credit) {
        return transactions.stream()
                .filter(t -> t.type().isCredit() == credit)
                .map(Transaction::amount)
                .reduce(Money.ZERO, Money::plus);
    }
}
