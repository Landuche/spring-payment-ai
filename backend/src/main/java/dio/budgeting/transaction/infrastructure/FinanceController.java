package dio.budgeting.transaction.infrastructure;

import dio.budgeting.investment.application.SimulateInvestmentUseCase;
import dio.budgeting.investment.application.SimulationOutput;
import dio.budgeting.investment.infrastructure.SimulationRequest;
import dio.budgeting.transaction.application.BalanceOutput;
import dio.budgeting.transaction.application.GetBalanceUseCase;
import dio.budgeting.transaction.application.ExpenseReportOutput;
import dio.budgeting.transaction.application.GenerateExpenseReportUseCase;
import dio.budgeting.transaction.domain.Category;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints diretos para o dashboard e o simulador (mesmos casos de uso das tools da IA). */
@RestController
@RequestMapping("/api")
public class FinanceController {
    private final GetBalanceUseCase getBalanceUseCase;
    private final GenerateExpenseReportUseCase generateExpenseReportUseCase;
    private final SimulateInvestmentUseCase simulateInvestmentUseCase;

    public FinanceController(GetBalanceUseCase getBalanceUseCase,
                             GenerateExpenseReportUseCase generateExpenseReportUseCase,
                             SimulateInvestmentUseCase simulateInvestmentUseCase) {
        this.getBalanceUseCase = getBalanceUseCase;
        this.generateExpenseReportUseCase = generateExpenseReportUseCase;
        this.simulateInvestmentUseCase = simulateInvestmentUseCase;
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

    @PostMapping("/simulations")
    public SimulationOutput simulate(@Valid @RequestBody SimulationRequest request) {
        return simulateInvestmentUseCase.execute(request.amount(), request.months());
    }
}
