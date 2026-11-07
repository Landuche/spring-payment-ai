package dio.budgeting.transfer.application;

import java.math.BigDecimal;

/**
 * Transferência que o Claudinho preparou e que só o cliente pode confirmar, tocando no botão do app.
 * {@code destinationAccount} no formato XXXX-XXXXXX; {@code recipientName} mascarado, como na busca de contas.
 */
public record TransferProposal(String destinationAccount, String recipientName, BigDecimal amount, String description) {
}
