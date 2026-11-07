package dio.budgeting.transfer.infrastructure;

import dio.budgeting.transfer.application.TransferOutput;
import dio.budgeting.transfer.application.TransferUseCase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
public class TransferController {
    static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final TransferUseCase transferUseCase;

    public TransferController(TransferUseCase transferUseCase) {
        this.transferUseCase = transferUseCase;
    }

    /** Informe exatamente um destino: destinationAccountNumber OU destinationEmail (validado no caso de uso). */
    public record TransferRequest(
            @Pattern(regexp = "\\d{4}-?\\d{6}", message = "número de conta inválido") String destinationAccountNumber,
            @Email @Size(max = 160) String destinationEmail,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
            @Size(max = 120) String description) {
    }

    /**
     * 201 na primeira execução; 200 quando a mesma Idempotency-Key já foi processada
     * (nenhum débito novo). Clientes podem repetir a chamada com segurança após timeouts.
     */
    @PostMapping("/transfers")
    public ResponseEntity<TransferOutput> transfer(
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        var output = transferUseCase.execute(request.destinationAccountNumber(), request.destinationEmail(),
                request.amount(), request.description(), idempotencyKey);
        return ResponseEntity.status(output.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(output);
    }
}
