package dio.budgeting.application.output;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code total} e {@code quantidade} contam só despesas. As transferências do período vêm à parte, para o relatório
 * explicar a variação do saldo sem inflar os gastos; ficam nulas quando o relatório é filtrado por categoria.
 */
public record ExpenseReportOutput(
        String periodo,
        String categoria,
        BigDecimal total,
        int quantidade,
        List<TransactionOutput> transacoes,
        BigDecimal transferenciasEnviadas,
        BigDecimal transferenciasRecebidas) {
}
