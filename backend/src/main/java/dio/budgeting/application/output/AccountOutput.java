package dio.budgeting.application.output;

/** numero no formato XXXX-XXXXXX; nomeTitular completo para a própria conta, mascarado para terceiros. */
public record AccountOutput(String numero, String nomeTitular) {
}
