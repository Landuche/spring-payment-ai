package dio.budgeting.infrastructure.http;

import dio.budgeting.application.CriarTransacaoUseCase;
import dio.budgeting.application.ListarTransacoesPorCategoriaUseCase;
import dio.budgeting.application.ListarTransacoesUseCase;
import dio.budgeting.domain.Category;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import dio.budgeting.infrastructure.http.response.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final CriarTransacaoUseCase criarTransacaoUseCase;
    private final ListarTransacoesPorCategoriaUseCase listarTransacoesPorCategoriaUseCase;

    private final ListarTransacoesUseCase listarTransacoesUseCase;

    public TransactionController(CriarTransacaoUseCase criarTransacaoUseCase,
                                 ListarTransacoesPorCategoriaUseCase listarTransacoesPorCategoriaUseCase,
                                 ListarTransacoesUseCase listarTransacoesUseCase) {
        this.criarTransacaoUseCase = criarTransacaoUseCase;
        this.listarTransacoesPorCategoriaUseCase = listarTransacoesPorCategoriaUseCase;
        this.listarTransacoesUseCase = listarTransacoesUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@Valid @RequestBody TransactionRequest request) {
        var output = criarTransacaoUseCase.execute(
                request.amount(), request.category(), request.type(), request.description());
        return TransactionResponse.from(output);
    }

    @GetMapping
    public List<TransactionResponse> listTransactions() {
        return listarTransacoesUseCase.execute().stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @GetMapping("/{category}")
    public List<TransactionResponse> readTransactions(@PathVariable Category category) {
        return listarTransacoesPorCategoriaUseCase.execute(category).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
