package dio.budgeting.transaction.infrastructure;

import dio.budgeting.transaction.application.CreateTransactionUseCase;
import dio.budgeting.transaction.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.transaction.application.ListTransactionsUseCase;
import dio.budgeting.transaction.domain.Category;

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
    private final CreateTransactionUseCase createTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;

    private final ListTransactionsUseCase listTransactionsUseCase;

    public TransactionController(CreateTransactionUseCase createTransactionUseCase,
                                 ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                                 ListTransactionsUseCase listTransactionsUseCase) {
        this.createTransactionUseCase = createTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.listTransactionsUseCase = listTransactionsUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@Valid @RequestBody TransactionRequest request) {
        var output = createTransactionUseCase.execute(
                request.amount(), request.category(), request.type(), request.description());
        return TransactionResponse.from(output);
    }

    @GetMapping
    public List<TransactionResponse> listTransactions() {
        return listTransactionsUseCase.execute().stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @GetMapping("/{category}")
    public List<TransactionResponse> readTransactions(@PathVariable Category category) {
        return listTransactionsByCategoryUseCase.execute(category).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
