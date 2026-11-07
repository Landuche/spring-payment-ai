package dio.budgeting.investment.infrastructure;

import dio.budgeting.investment.application.SimulateInvestmentUseCase;
import dio.budgeting.investment.application.SimulationOutput;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint do simulador (mesmo caso de uso da ferramenta do assistente). */
@RestController
@RequestMapping("/api")
public class InvestmentController {
    private final SimulateInvestmentUseCase simulateInvestmentUseCase;

    public InvestmentController(SimulateInvestmentUseCase simulateInvestmentUseCase) {
        this.simulateInvestmentUseCase = simulateInvestmentUseCase;
    }

    @PostMapping("/simulations")
    public SimulationOutput simulate(@Valid @RequestBody SimulationRequest request) {
        return simulateInvestmentUseCase.execute(request.amount(), request.months());
    }
}
