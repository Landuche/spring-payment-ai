package dio.budgeting.transaction.application;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code total} e {@code count} contam só despesas. As transferências do período vêm separado
 */
public record ExpenseReportOutput(
        String period,
        String category,
        BigDecimal total,
        int count,
        List<TransactionOutput> transactions,
        BigDecimal sentTransfers,
        BigDecimal receivedTransfers) {
}
