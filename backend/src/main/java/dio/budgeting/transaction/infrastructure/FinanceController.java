package dio.budgeting.transaction.infrastructure;

import dio.budgeting.investment.application.SimularInvestimentoUseCase;
import dio.budgeting.investment.application.SimulationOutput;
import dio.budgeting.investment.infrastructure.SimulationRequest;
import dio.budgeting.transaction.application.BalanceOutput;
import dio.budgeting.transaction.application.ConsultarSaldoUseCase;
import dio.budgeting.transaction.application.ExpenseReportOutput;
import dio.budgeting.transaction.application.GerarRelatorioGastosUseCase;
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
    private final ConsultarSaldoUseCase consultarSaldoUseCase;
    private final GerarRelatorioGastosUseCase gerarRelatorioGastosUseCase;
    private final SimularInvestimentoUseCase simularInvestimentoUseCase;

    public FinanceController(ConsultarSaldoUseCase consultarSaldoUseCase,
                             GerarRelatorioGastosUseCase gerarRelatorioGastosUseCase,
                             SimularInvestimentoUseCase simularInvestimentoUseCase) {
        this.consultarSaldoUseCase = consultarSaldoUseCase;
        this.gerarRelatorioGastosUseCase = gerarRelatorioGastosUseCase;
        this.simularInvestimentoUseCase = simularInvestimentoUseCase;
    }

    @GetMapping("/balance")
    public BalanceOutput balance() {
        return consultarSaldoUseCase.execute();
    }

    @GetMapping("/reports/expenses")
    public ExpenseReportOutput expenses(@RequestParam String periodo,
                                        @RequestParam(required = false) Category categoria) {
        return gerarRelatorioGastosUseCase.execute(periodo, categoria);
    }

    @PostMapping("/simulations")
    public SimulationOutput simulate(@Valid @RequestBody SimulationRequest request) {
        return simularInvestimentoUseCase.execute(request.amount(), request.months());
    }
}
