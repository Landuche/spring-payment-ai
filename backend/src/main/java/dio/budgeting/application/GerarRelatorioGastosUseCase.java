package dio.budgeting.application;

import dio.budgeting.application.output.ExpenseReportOutput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.application.port.CurrentAccountProvider;
import dio.budgeting.domain.BusinessRuleViolationException;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Money;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.domain.TransactionType;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class GerarRelatorioGastosUseCase {
    private static final String TODAS_CATEGORIAS = "TODAS";

    private final TransactionRepository transactionRepository;
    private final CurrentAccountProvider currentAccount;

    public GerarRelatorioGastosUseCase(TransactionRepository transactionRepository, CurrentAccountProvider currentAccount) {
        this.transactionRepository = transactionRepository;
        this.currentAccount = currentAccount;
    }

    @Tool(name = "gerarRelatorioGastos",
            description = "Gera o relatório de despesas de um mês, opcionalmente filtrado por categoria. "
                    + "O total e a lista contam só despesas. Sem filtro, informa também o total de transferências "
                    + "enviadas e recebidas no mês, que NÃO fazem parte do total de gastos")
    public ExpenseReportOutput execute(
            @ToolParam(description = "Mês de referência, no formato interno yyyy-MM (ex: 2026-10). Se o cliente disser "
                    + "\"este mês\" ou não indicar o mês, use o mês atual. Nunca peça nem mostre esse formato ao cliente") String periodo,
            @ToolParam(description = "Categoria para filtrar. Se omitida, considera todas", required = false)
            Category categoria) {

        var month = parsePeriodo(periodo);
        var accountId = currentAccount.current();

        List<Transaction> transactions = categoria == null
                ? transactionRepository.findAllBetween(accountId, month.atDay(1), month.atEndOfMonth())
                : transactionRepository.findAllBetween(accountId, month.atDay(1), month.atEndOfMonth(), categoria);

        var expenses = transactions.stream()
                .filter(t -> t.type() == TransactionType.DESPESA)
                .toList();

        // Transferências não são gasto: ficam fora do total, mas aparecem à parte quando o relatório é geral.
        var filtradoPorCategoria = categoria != null;

        return new ExpenseReportOutput(
                month.toString(),
                filtradoPorCategoria ? categoria.name() : TODAS_CATEGORIAS,
                soma(expenses, TransactionType.DESPESA).toReais(),
                expenses.size(),
                expenses.stream().map(TransactionOutput::from).toList(),
                filtradoPorCategoria ? null : soma(transactions, TransactionType.TRANSFERENCIA_ENVIADA).toReais(),
                filtradoPorCategoria ? null : soma(transactions, TransactionType.TRANSFERENCIA_RECEBIDA).toReais());
    }

    private static Money soma(List<Transaction> transactions, TransactionType tipo) {
        return transactions.stream()
                .filter(t -> t.type() == tipo)
                .map(Transaction::amount)
                .reduce(Money.ZERO, Money::plus);
    }

    private static YearMonth parsePeriodo(String periodo) {
        try {
            return YearMonth.parse(periodo);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessRuleViolationException("Período inválido, use o formato yyyy-MM");
        }
    }
}
