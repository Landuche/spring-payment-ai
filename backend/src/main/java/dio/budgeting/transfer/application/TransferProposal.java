package dio.budgeting.transfer.application;

import java.math.BigDecimal;

/**
 * Transferência que o Claudinho preparou e que só o cliente pode confirmar
 */
public record TransferProposal(String destinationAccount, String recipientName, BigDecimal amount, String description) {
}
