package dio.budgeting.assistant.infrastructure;

import dio.budgeting.investment.application.SimulateInvestmentUseCase;
import dio.budgeting.investment.application.SimulationOutput;
import dio.budgeting.transaction.application.BalanceOutput;
import dio.budgeting.transaction.application.GetBalanceUseCase;
import dio.budgeting.transaction.application.CreateTransactionUseCase;
import dio.budgeting.transaction.application.ExpenseReportOutput;
import dio.budgeting.transaction.application.GenerateExpenseReportUseCase;
import dio.budgeting.transaction.application.TransactionOutput;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.TransactionType;
import dio.budgeting.transfer.application.PrepareTransferUseCase;

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
    /** Chave do contexto de ferramentas onde o {@link AssistantChat} coloca as {@link TurnProposals}. */
    static final String CONTEXT_PROPOSALS = "proposals";

    private static final String AWAITING_CONFIRMATION = "AWAITING_CUSTOMER_CONFIRMATION";

    private final CreateTransactionUseCase createTransaction;
    private final GetBalanceUseCase getBalance;
    private final GenerateExpenseReportUseCase generateExpenseReport;
    private final SimulateInvestmentUseCase simulateInvestment;
    private final PrepareTransferUseCase prepareTransfer;

    public AssistantTools(CreateTransactionUseCase createTransaction,
                          GetBalanceUseCase getBalance,
                          GenerateExpenseReportUseCase generateExpenseReport,
                          SimulateInvestmentUseCase simulateInvestment,
                          PrepareTransferUseCase prepareTransfer) {
        this.createTransaction = createTransaction;
        this.getBalance = getBalance;
        this.generateExpenseReport = generateExpenseReport;
        this.simulateInvestment = simulateInvestment;
        this.prepareTransfer = prepareTransfer;
    }

    @Tool(name = "createTransaction",
            description = "Registra uma nova receita ou despesa na data de hoje. Não serve para transferências entre contas")
    public TransactionOutput createTransaction(
            @ToolParam(description = "Valor em reais, maior que zero. Ex: 45.90") BigDecimal amount,
            @ToolParam(description = "Categoria da transação. GROCERIES: mercado, padaria, hortifruti. PHARMA: farmácia, remédios. "
                    + "AUTO: combustível, estacionamento, oficina, IPVA, carro. OTHER: qualquer outro gasto, como adega, bar, restaurante, lazer. "
                    + "Use OTHER quando nenhuma outra categoria se encaixar. Nunca recuse um gasto por causa da categoria.") Category category,
            @ToolParam(description = "Tipo da transação: INCOME ou EXPENSE") TransactionType type,
            @ToolParam(description = "Descrição curta do gasto ou da receita. Opcional", required = false)
            String description) {
        return createTransaction.execute(amount, category, type, description);
    }

    @Tool(name = "getBalance",
            description = "Consulta o saldo atual da conta. O resultado traz também receitas e despesas, mas ao "
                    + "responder informe apenas o saldo, a menos que o cliente peça as entradas e saídas")
    public BalanceOutput getBalance() {
        return getBalance.execute();
    }

    @Tool(name = "generateExpenseReport",
            description = "Gera o relatório de despesas de um mês, opcionalmente filtrado por categoria. "
                    + "O total e a lista contam só despesas. Sem filtro, informa também o total de transferências "
                    + "enviadas e recebidas no mês, que NÃO fazem parte do total de gastos")
    public ExpenseReportOutput generateExpenseReport(
            @ToolParam(description = "Mês de referência, no formato interno yyyy-MM (ex: 2026-10). Se o cliente disser "
                    + "\"este mês\" ou não indicar o mês, use o mês atual. Nunca peça nem mostre esse formato ao cliente") String period,
            @ToolParam(description = "Categoria para filtrar. Se omitida, considera todas", required = false)
            Category category) {
        return generateExpenseReport.execute(period, category);
    }

    @Tool(name = "simulateInvestment",
            description = "Simula o montante final de um investimento com juros compostos mensais")
    public SimulationOutput simulateInvestment(
            @ToolParam(description = "Valor a investir em reais, maior que zero. Ex: 1000") BigDecimal amount,
            @ToolParam(description = "Prazo do investimento em meses, entre 1 e 360") int termMonths) {
        return simulateInvestment.execute(amount, termMonths);
    }

    @Tool(name = "prepareTransfer",
            description = "Prepara uma transferência de dinheiro para outra pessoa. NÃO executa: o cliente confirma tocando "
                    + "num botão que aparece no chat. Use quando o cliente quiser enviar dinheiro a alguém e informar o "
                    + "e-mail ou o número da conta do destinatário e o valor. Depois de chamar, diga que a transferência "
                    + "foi preparada e precisa ser confirmada no cartão; nunca diga que foi enviada")
    public TransferPreparation prepareTransfer(
            @ToolParam(description = "E-mail do titular da conta de destino OU o número da conta (10 dígitos, "
                    + "ex: 1234-567890). Se o cliente ditou o e-mail ('arroba', 'ponto'), converta para o formato normal")
            String recipient,
            @ToolParam(description = "Valor em reais, maior que zero. Ex: 50.00") BigDecimal amount,
            @ToolParam(description = "Descrição curta da transferência. Opcional", required = false) String description,
            ToolContext toolContext) {

        var proposal = prepareTransfer.execute(recipient, amount, description);
        proposalsOf(toolContext).register(proposal);

        return new TransferPreparation(AWAITING_CONFIRMATION, proposal.recipientName(), proposal.destinationAccount(),
                proposal.amount(),
                "A transferência AINDA NÃO foi feita. O cliente precisa confirmar tocando no botão do cartão que "
                        + "aparece no chat. Avise isso em uma frase, não diga que foi enviada e NÃO sugira nenhuma outra "
                        + "ação agora: espere o cliente decidir no cartão (o app vai te avisar o resultado).");
    }

    private static TurnProposals proposalsOf(ToolContext toolContext) {
        if (toolContext != null && toolContext.getContext().get(CONTEXT_PROPOSALS) instanceof TurnProposals proposals) {
            return proposals;
        }
        throw new IllegalStateException("Contexto de propostas ausente: o AssistantChat precisa fornecê-lo à ferramenta");
    }
}
