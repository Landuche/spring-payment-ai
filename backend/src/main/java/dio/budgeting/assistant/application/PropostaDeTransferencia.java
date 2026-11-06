package dio.budgeting.assistant.application;

import java.math.BigDecimal;

/**
 * Transferência que o Claudinho preparou e que só o cliente pode confirmar, tocando no botão do app.
 * {@code contaDestino} no formato XXXX-XXXXXX; {@code nomeDestinatario} mascarado, como na busca de contas.
 */
public record PropostaDeTransferencia(String contaDestino, String nomeDestinatario, BigDecimal valor, String descricao) {
}
