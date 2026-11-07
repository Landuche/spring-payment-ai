package dio.budgeting.assistant.infrastructure;

import dio.budgeting.TestData;
import dio.budgeting.account.application.GetAccountUseCase;
import dio.budgeting.account.application.InMemoryAccountRepository;
import dio.budgeting.account.application.InMemoryLookupGuardRepository;
import dio.budgeting.account.application.LookupGuardService;
import dio.budgeting.account.domain.Account;
import dio.budgeting.audit.application.AuditLog;
import dio.budgeting.audit.application.InMemoryAuditEventRepository;
import dio.budgeting.auth.application.InMemoryUserRepository;
import dio.budgeting.investment.application.SimulateInvestmentUseCase;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.transaction.application.GetBalanceUseCase;
import dio.budgeting.transaction.application.CreateTransactionUseCase;
import dio.budgeting.transaction.application.GenerateExpenseReportUseCase;
import dio.budgeting.transaction.application.InMemoryTransactionRepository;
import dio.budgeting.transfer.application.InMemoryTransferRepository;
import dio.budgeting.transfer.application.PrepareTransferUseCase;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Garante o contrato com o modelo: nomes e parâmetros das ferramentas, e que preparar uma transferência não a executa. */
class AssistantToolsTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);
    private final InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
    private final InMemoryTransferRepository transfers = new InMemoryTransferRepository();
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final AuditLog audit = new AuditLog(new InMemoryAuditEventRepository(), clock);
    private final LookupGuardService lookupGuard =
            new LookupGuardService(new InMemoryLookupGuardRepository(), accounts, audit, clock);

    private final Account source = TestData.newAccount(users, accounts, "Ana Souza", "0000000001");
    private final Account destination = TestData.newAccount(users, accounts, "Bruno Lima", "0000000002");

    private final AssistantTools tools = new AssistantTools(
            new CreateTransactionUseCase(transactions, accounts, () -> source.id(), clock),
            new GetBalanceUseCase(transactions, () -> source.id()),
            new GenerateExpenseReportUseCase(transactions, () -> source.id()),
            new SimulateInvestmentUseCase(new BigDecimal("0.008")),
            new PrepareTransferUseCase(
                    new GetAccountUseCase(accounts, users, () -> source.id(), lookupGuard),
                    new GetBalanceUseCase(transactions, () -> source.id())));

    private final TurnProposals proposals = new TurnProposals();
    private final ToolContext context = new ToolContext(Map.of(AssistantTools.CONTEXT_PROPOSALS, proposals));

    @Test
    void should_exposeTheFiveToolsToTheModel() {
        var names = Arrays.stream(ToolCallbacks.from(tools)).map(t -> t.getToolDefinition().name()).toList();

        assertThat(names).containsExactlyInAnyOrder(
                "createTransaction", "getBalance", "generateExpenseReport", "simulateInvestment",
                "prepareTransfer");
    }

    @Test
    void should_hideTheContextFromTheModelSchema_andReceiveItWhenTheModelCallsTheTool() {
        TestData.deposit(transactions, source, "100.00");
        var tool = tool("prepareTransfer");

        assertThat(tool.getToolDefinition().inputSchema())
                .contains("recipient", "amount", "description")
                .doesNotContain("toolContext");

        var call = "{\"recipient\":\"0000-000002\",\"amount\":30.00,\"description\":\"Pizza\"}";
        var result = tool.call(call, context);

        assertThat(result).contains("AWAITING_CUSTOMER_CONFIRMATION").contains("AINDA NÃO");
        var proposal = proposals.transfer().orElseThrow();
        assertThat(proposal.destinationAccount()).isEqualTo("0000-000002");
        assertThat(proposal.recipientName()).isEqualTo("Bruno L.");
        // Preparar não move dinheiro.
        assertThat(transfers.count()).isZero();
        assertThat(transactions.findAll(source.id())).hasSize(1);
        assertThat(transactions.findAll(destination.id())).isEmpty();
    }

    @Test
    void should_keepOnlyTheLastProposal_whenModelPreparesTwice() {
        TestData.deposit(transactions, source, "100.00");

        tools.prepareTransfer("0000000002", new BigDecimal("10.00"), null, context);
        tools.prepareTransfer("0000000002", new BigDecimal("25.00"), null, context);

        assertThat(proposals.transfer().orElseThrow().amount()).isEqualByComparingTo("25.00");
    }

    @Test
    void should_createNoProposal_whenThePreparationIsRefused() {
        TestData.deposit(transactions, source, "10.00");

        assertThatThrownBy(() -> tools.prepareTransfer("0000000002", new BigDecimal("50.00"), null, context))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Saldo insuficiente");
        assertThat(proposals.transfer()).isEmpty();
    }

    @Test
    void should_failClearly_whenTheProposalContextIsMissing() {
        TestData.deposit(transactions, source, "100.00");

        assertThatThrownBy(() -> tools.prepareTransfer("0000000002", new BigDecimal("10.00"), null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_delegateBalanceToTheUseCase() {
        TestData.deposit(transactions, source, "42.00");

        assertThat(tools.getBalance().balance()).isEqualByComparingTo("42.00");
    }

    private ToolCallback tool(String name) {
        return Arrays.stream(ToolCallbacks.from(tools))
                .filter(t -> t.getToolDefinition().name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
