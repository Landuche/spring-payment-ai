package dio.budgeting.assistant.application;

import java.math.BigDecimal;

/** Resultado da ferramenta para o modelo: a transferência foi só preparada, não executada. */
public record PreparoDeTransferencia(String situacao, String destinatario, String conta, BigDecimal valor, String aviso) {
}
