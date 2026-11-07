package dio.budgeting.transaction.infrastructure;

import dio.budgeting.transaction.application.BalanceOutput;
import dio.budgeting.transaction.application.GetBalanceUseCase;
import dio.budgeting.transaction.application.ExpenseReportOutput;
import dio.budgeting.transaction.application.GenerateExpenseReportUseCase;
import dio.budgeting.transaction.domain.Category;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints diretos para o painel: saldo e relatório de gastos (mesmos casos de uso das ferramentas do assistente). */
@RestController
@RequestMapping("/api")
public class FinanceController {
    private final GetBalanceUseCase getBalanceUseCase;
    private final GenerateExpenseReportUseCase generateExpenseReportUseCase;

    public FinanceController(GetBalanceUseCase getBalanceUseCase,
                             GenerateExpenseReportUseCase generateExpenseReportUseCase) {
        this.getBalanceUseCase = getBalanceUseCase;
        this.generateExpenseReportUseCase = generateExpenseReportUseCase;
    }

    @GetMapping("/balance")
    public BalanceOutput balance() {
        return getBalanceUseCase.execute();
    }

    @GetMapping("/reports/expenses")
    public ExpenseReportOutput expenses(@RequestParam String period,
                                        @RequestParam(required = false) Category category) {
        return generateExpenseReportUseCase.execute(period, category);
    }
}
