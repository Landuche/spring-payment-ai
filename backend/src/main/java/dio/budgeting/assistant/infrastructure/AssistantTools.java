package dio.budgeting.assistant.infrastructure;

import dio.budgeting.investment.application.SimularInvestimentoUseCase;
import dio.budgeting.investment.application.SimulationOutput;
import dio.budgeting.transaction.application.BalanceOutput;
import dio.budgeting.transaction.application.ConsultarSaldoUseCase;
import dio.budgeting.transaction.application.CriarTransacaoUseCase;
import dio.budgeting.transaction.application.ExpenseReportOutput;
import dio.budgeting.transaction.application.GerarRelatorioGastosUseCase;
import dio.budgeting.transaction.application.TransactionOutput;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.TransactionType;
import dio.budgeting.transfer.application.PrepararTransferenciaUseCase;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Ferramentas que o modelo pode chamar. É o único ponto que conhece o Spring AI: cada método só traduz a chamada do
 * modelo para o caso de uso correspondente, que continua sem saber que existe uma IA. A IA nunca acessa repositórios.
 */
@Component
public class AssistantTools {
    /** Chave do contexto de ferramentas onde o {@link AssistantChat} coloca as {@link PropostasDoTurno}. */
    static final String CONTEXT_PROPOSALS = "propostas";

    private static final String AWAITING_CONFIRMATION = "AGUARDANDO_CONFIRMACAO_DO_CLIENTE";

    private final CriarTransacaoUseCase criarTransacao;
    private final ConsultarSaldoUseCase consultarSaldo;
    private final GerarRelatorioGastosUseCase gerarRelatorioGastos;
    private final SimularInvestimentoUseCase simularInvestimento;
    private final PrepararTransferenciaUseCase prepararTransferencia;

    public AssistantTools(CriarTransacaoUseCase criarTransacao,
                          ConsultarSaldoUseCase consultarSaldo,
                          GerarRelatorioGastosUseCase gerarRelatorioGastos,
                          SimularInvestimentoUseCase simularInvestimento,
                          PrepararTransferenciaUseCase prepararTransferencia) {
        this.criarTransacao = criarTransacao;
        this.consultarSaldo = consultarSaldo;
        this.gerarRelatorioGastos = gerarRelatorioGastos;
        this.simularInvestimento = simularInvestimento;
        this.prepararTransferencia = prepararTransferencia;
    }

    @Tool(name = "criarTransacao",
            description = "Registra uma nova receita ou despesa na data de hoje. Não serve para transferências entre contas")
    public TransactionOutput criarTransacao(
            @ToolParam(description = "Valor em reais, maior que zero. Ex: 45.90") BigDecimal valor,
            @ToolParam(description = "Categoria da transação. GROCERIES: mercado, padaria, hortifruti. PHARMA: farmácia, remédios. "
                    + "AUTO: combustível, estacionamento, oficina, IPVA, carro. OUTROS: qualquer outro gasto, como adega, bar, restaurante, lazer. "
                    + "Use OUTROS quando nenhuma outra categoria se encaixar. Nunca recuse um gasto por causa da categoria.") Category categoria,
            @ToolParam(description = "Tipo da transação: RECEITA ou DESPESA") TransactionType tipo,
            @ToolParam(description = "Descrição curta do gasto ou da receita. Opcional", required = false)
            String descricao) {
        return criarTransacao.execute(valor, categoria, tipo, descricao);
    }

    @Tool(name = "consultarSaldo",
            description = "Consulta o saldo atual da conta. O resultado traz também receitas e despesas, mas ao "
                    + "responder informe apenas o saldo, a menos que o cliente peça as entradas e saídas")
    public BalanceOutput consultarSaldo() {
        return consultarSaldo.execute();
    }

    @Tool(name = "gerarRelatorioGastos",
            description = "Gera o relatório de despesas de um mês, opcionalmente filtrado por categoria. "
                    + "O total e a lista contam só despesas. Sem filtro, informa também o total de transferências "
                    + "enviadas e recebidas no mês, que NÃO fazem parte do total de gastos")
    public ExpenseReportOutput gerarRelatorioGastos(
            @ToolParam(description = "Mês de referência, no formato interno yyyy-MM (ex: 2026-10). Se o cliente disser "
                    + "\"este mês\" ou não indicar o mês, use o mês atual. Nunca peça nem mostre esse formato ao cliente") String periodo,
            @ToolParam(description = "Categoria para filtrar. Se omitida, considera todas", required = false)
            Category categoria) {
        return gerarRelatorioGastos.execute(periodo, categoria);
    }

    @Tool(name = "simularInvestimento",
            description = "Simula o montante final de um investimento com juros compostos mensais")
    public SimulationOutput simularInvestimento(
            @ToolParam(description = "Valor a investir em reais, maior que zero. Ex: 1000") BigDecimal valor,
            @ToolParam(description = "Prazo do investimento em meses, entre 1 e 360") int prazo) {
        return simularInvestimento.execute(valor, prazo);
    }

    @Tool(name = "prepararTransferencia",
            description = "Prepara uma transferência de dinheiro para outra pessoa. NÃO executa: o cliente confirma tocando "
                    + "num botão que aparece no chat. Use quando o cliente quiser enviar dinheiro a alguém e informar o "
                    + "e-mail ou o número da conta do destinatário e o valor. Depois de chamar, diga que a transferência "
                    + "foi preparada e precisa ser confirmada no cartão; nunca diga que foi enviada")
    public PreparoDeTransferencia prepararTransferencia(
            @ToolParam(description = "E-mail do titular da conta de destino OU o número da conta (10 dígitos, "
                    + "ex: 1234-567890). Se o cliente ditou o e-mail ('arroba', 'ponto'), converta para o formato normal")
            String destinatario,
            @ToolParam(description = "Valor em reais, maior que zero. Ex: 50.00") BigDecimal valor,
            @ToolParam(description = "Descrição curta da transferência. Opcional", required = false) String descricao,
            ToolContext toolContext) {

        var proposta = prepararTransferencia.execute(destinatario, valor, descricao);
        proposalsOf(toolContext).registrar(proposta);

        return new PreparoDeTransferencia(AWAITING_CONFIRMATION, proposta.nomeDestinatario(), proposta.contaDestino(),
                proposta.valor(),
                "A transferência AINDA NÃO foi feita. O cliente precisa confirmar tocando no botão do cartão que "
                        + "aparece no chat. Avise isso em uma frase, não diga que foi enviada e NÃO sugira nenhuma outra "
                        + "ação agora: espere o cliente decidir no cartão (o app vai te avisar o resultado).");
    }

    private static PropostasDoTurno proposalsOf(ToolContext toolContext) {
        if (toolContext != null && toolContext.getContext().get(CONTEXT_PROPOSALS) instanceof PropostasDoTurno propostas) {
            return propostas;
        }
        throw new IllegalStateException("Contexto de propostas ausente: o AssistantChat precisa fornecê-lo à ferramenta");
    }
}
