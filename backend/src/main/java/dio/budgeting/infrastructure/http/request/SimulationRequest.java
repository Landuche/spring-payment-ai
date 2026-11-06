package dio.budgeting.infrastructure.http.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SimulationRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @Min(1) @Max(360) int months) {
}
