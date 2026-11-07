package dio.budgeting.account.application;

/** {@code number} no formato XXXX-XXXXXX; {@code holderName} completo na própria conta e mascarado para terceiros. */
public record AccountOutput(String number, String holderName) {
}
