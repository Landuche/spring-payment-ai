package dio.budgeting.account.application;

/** numero no formato XXXX-XXXXXX; nomeTitular completo para a própria conta, mascarado para terceiros. */
public record AccountOutput(String number, String holderName) {
}
