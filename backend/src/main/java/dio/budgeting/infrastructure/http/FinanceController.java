package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ConsultarSaldoUseCase;
import dio.budgeting.application.GerarRelatorioGastosUseCase;
import dio.budgeting.application.SimularInvestimentoUseCase;
import dio.budgeting.application.output.BalanceOutput;
import dio.budgeting.application.output.ExpenseReportOutput;
import dio.budgeting.application.output.SimulationOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.infrastructure.http.request.SimulationRequest;
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
