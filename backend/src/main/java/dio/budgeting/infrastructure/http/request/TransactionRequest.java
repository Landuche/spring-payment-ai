package dio.budgeting.infrastructure.http.request;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransactionRequest(
        @Size(max = 120) String description,
        @NotNull Category category,
        @NotNull TransactionType type,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount) {
}
