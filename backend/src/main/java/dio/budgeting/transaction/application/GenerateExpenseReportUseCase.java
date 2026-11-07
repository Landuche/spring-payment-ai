package dio.budgeting.transaction.application;

import dio.budgeting.account.application.CurrentAccountProvider;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionRepository;
import dio.budgeting.transaction.domain.TransactionType;

import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class GenerateExpenseReportUseCase {
    private static final String ALL_CATEGORIES = "ALL";

    private final TransactionRepository transactionRepository;
    private final CurrentAccountProvider currentAccount;

    public GenerateExpenseReportUseCase(TransactionRepository transactionRepository, CurrentAccountProvider currentAccount) {
        this.transactionRepository = transactionRepository;
        this.currentAccount = currentAccount;
    }

    public ExpenseReportOutput execute(String period, Category category) {

        var month = parsePeriod(period);
        var accountId = currentAccount.current();

        List<Transaction> transactions = category == null
                ? transactionRepository.findAllBetween(accountId, month.atDay(1), month.atEndOfMonth())
                : transactionRepository.findAllBetween(accountId, month.atDay(1), month.atEndOfMonth(), category);

        var expenses = transactions.stream()
                .filter(t -> t.type() == TransactionType.EXPENSE)
                .toList();

        // Transferências não são gasto: ficam fora do total, mas aparecem à parte quando o relatório é geral.
        var filtradoPorCategoria = category != null;

        return new ExpenseReportOutput(
                month.toString(),
                filtradoPorCategoria ? category.name() : ALL_CATEGORIES,
                sum(expenses, TransactionType.EXPENSE).toReais(),
                expenses.size(),
                expenses.stream().map(TransactionOutput::from).toList(),
                filtradoPorCategoria ? null : sum(transactions, TransactionType.TRANSFER_SENT).toReais(),
                filtradoPorCategoria ? null : sum(transactions, TransactionType.TRANSFER_RECEIVED).toReais());
    }

    private static Money sum(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.type() == type)
                .map(Transaction::amount)
                .reduce(Money.ZERO, Money::plus);
    }

    private static YearMonth parsePeriod(String period) {
        try {
            return YearMonth.parse(period);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessRuleViolationException("Período inválido, use o formato yyyy-MM");
        }
    }
}
