package dio.budgeting.assistant.infrastructure;

import java.math.BigDecimal;

/** Result da ferramenta para o modelo: a transferência foi só preparada, não executada. */
public record TransferPreparation(String status, String recipient, String account, BigDecimal amount, String warning) {
}
