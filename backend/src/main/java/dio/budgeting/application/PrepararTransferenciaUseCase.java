package dio.budgeting.application;

import dio.budgeting.application.output.AccountOutput;
import dio.budgeting.application.output.PreparoDeTransferencia;
import dio.budgeting.application.output.PropostaDeTransferencia;
import dio.budgeting.domain.BusinessRuleViolationException;
import dio.budgeting.domain.Money;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Prepara uma transferência pelo Claudinho. NÃO move dinheiro: localiza o destinatário, confere saldo e conta própria
 * e registra uma proposta, que o app mostra como cartão de confirmação. Quem executa é o cliente, tocando no botão,
 * pelo mesmo endpoint de transferência do app (com chave de idempotência e todas as validações). Assim o modelo
 * nunca é quem decide mover dinheiro.
 */
@Service
public class PrepararTransferenciaUseCase {
    /** Chave do contexto de ferramentas onde o controller coloca as {@link PropostasDoTurno}. */
    public static final String CONTEXTO = "propostas";

    private final ConsultarContaUseCase consultarConta;
    private final ConsultarSaldoUseCase consultarSaldo;

    public PrepararTransferenciaUseCase(ConsultarContaUseCase consultarConta, ConsultarSaldoUseCase consultarSaldo) {
        this.consultarConta = consultarConta;
        this.consultarSaldo = consultarSaldo;
    }

    @Tool(name = "prepararTransferencia",
            description = "Prepara uma transferência de dinheiro para outra pessoa. NÃO executa: o cliente confirma tocando "
                    + "num botão que aparece no chat. Use quando o cliente quiser enviar dinheiro a alguém e informar o "
                    + "e-mail ou o número da conta do destinatário e o valor. Depois de chamar, diga que a transferência "
                    + "foi preparada e precisa ser confirmada no cartão; nunca diga que foi enviada")
    public PreparoDeTransferencia execute(
            @ToolParam(description = "E-mail do titular da conta de destino OU o número da conta (10 dígitos, "
                    + "ex: 1234-567890). Se o cliente ditou o e-mail ('arroba', 'ponto'), converta para o formato normal")
            String destinatario,
            @ToolParam(description = "Valor em reais, maior que zero. Ex: 50.00") BigDecimal valor,
            @ToolParam(description = "Descrição curta da transferência. Opcional", required = false) String descricao,
            ToolContext toolContext) {

        var amount = Money.ofPositiveReais(valor);
        var recipient = localizar(destinatario);

        if (recipient.numero().equals(consultarConta.minhaConta().numero())) {
            throw new BusinessRuleViolationException("Não é possível transferir para a própria conta");
        }
        var saldo = consultarSaldo.execute().saldo();
        if (amount.toReais().compareTo(saldo) > 0) {
            throw new BusinessRuleViolationException(
                    "Saldo insuficiente: saldo atual de " + saldo + " reais não cobre a transferência de "
                            + amount.toReais() + " reais");
        }

        var texto = descricao == null || descricao.isBlank() ? null : descricao.strip();
        var proposta = new PropostaDeTransferencia(recipient.numero(), recipient.nomeTitular(), amount.toReais(), texto);
        propostasDo(toolContext).registrar(proposta);

        return new PreparoDeTransferencia("AGUARDANDO_CONFIRMACAO_DO_CLIENTE", recipient.nomeTitular(),
                recipient.numero(), amount.toReais(),
                "A transferência AINDA NÃO foi feita. O cliente precisa confirmar tocando no botão do cartão que "
                        + "aparece no chat. Avise isso em uma frase, não diga que foi enviada e NÃO sugira nenhuma outra "
                        + "ação agora: espere o cliente decidir no cartão (o app vai te avisar o resultado).");
    }

    private AccountOutput localizar(String destinatario) {
        var texto = destinatario == null ? "" : destinatario.strip();
        return texto.contains("@") ? consultarConta.buscarPorEmail(texto) : consultarConta.buscarPorNumero(texto);
    }

    private static PropostasDoTurno propostasDo(ToolContext toolContext) {
        if (toolContext != null && toolContext.getContext().get(CONTEXTO) instanceof PropostasDoTurno propostas) {
            return propostas;
        }
        throw new IllegalStateException("Contexto de propostas ausente: o controller precisa fornecê-lo à ferramenta");
    }
}
