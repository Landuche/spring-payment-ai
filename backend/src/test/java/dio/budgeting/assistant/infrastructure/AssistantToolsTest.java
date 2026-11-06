package dio.budgeting.assistant.infrastructure;

import dio.budgeting.account.application.ConsultarContaUseCase;
import dio.budgeting.account.application.InMemoryAccountRepository;
import dio.budgeting.account.application.InMemoryLookupGuardRepository;
import dio.budgeting.account.application.LookupGuardService;
import dio.budgeting.account.domain.Account;
import dio.budgeting.account.domain.AccountId;
import dio.budgeting.audit.application.AuditLog;
import dio.budgeting.audit.application.InMemoryAuditEventRepository;
import dio.budgeting.auth.application.InMemoryUserRepository;
import dio.budgeting.auth.domain.User;
import dio.budgeting.auth.domain.UserId;
import dio.budgeting.investment.application.SimularInvestimentoUseCase;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.application.ConsultarSaldoUseCase;
import dio.budgeting.transaction.application.CriarTransacaoUseCase;
import dio.budgeting.transaction.application.GerarRelatorioGastosUseCase;
import dio.budgeting.transaction.application.InMemoryTransactionRepository;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionType;
import dio.budgeting.transfer.application.InMemoryTransferRepository;
import dio.budgeting.transfer.application.PrepararTransferenciaUseCase;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

    private final Account origem = novaConta("Ana Souza", "0000000001");
    private final Account destino = novaConta("Bruno Lima", "0000000002");

    private final AssistantTools tools = new AssistantTools(
            new CriarTransacaoUseCase(transactions, accounts, () -> origem.id(), clock),
            new ConsultarSaldoUseCase(transactions, () -> origem.id()),
            new GerarRelatorioGastosUseCase(transactions, () -> origem.id()),
            new SimularInvestimentoUseCase(new BigDecimal("0.008")),
            new PrepararTransferenciaUseCase(
                    new ConsultarContaUseCase(accounts, users, () -> origem.id(), lookupGuard),
                    new ConsultarSaldoUseCase(transactions, () -> origem.id())));

    private final PropostasDoTurno propostas = new PropostasDoTurno();
    private final ToolContext contexto = new ToolContext(Map.of(AssistantTools.CONTEXT_PROPOSALS, propostas));

    @Test
    void should_exposeTheFiveToolsToTheModel() {
        var nomes = Arrays.stream(ToolCallbacks.from(tools)).map(t -> t.getToolDefinition().name()).toList();

        assertThat(nomes).containsExactlyInAnyOrder(
                "criarTransacao", "consultarSaldo", "gerarRelatorioGastos", "simularInvestimento",
                "prepararTransferencia");
    }

    @Test
    void should_hideTheContextFromTheModelSchema_andReceiveItWhenTheModelCallsTheTool() {
        depositar(origem, "100.00");
        var tool = ferramenta("prepararTransferencia");

        assertThat(tool.getToolDefinition().inputSchema())
                .contains("destinatario", "valor", "descricao")
                .doesNotContain("toolContext");

        var chamada = "{\"destinatario\":\"0000-000002\",\"valor\":30.00,\"descricao\":\"Pizza\"}";
        var resultado = tool.call(chamada, contexto);

        assertThat(resultado).contains("AGUARDANDO_CONFIRMACAO_DO_CLIENTE").contains("AINDA NÃO");
        var proposta = propostas.transferencia().orElseThrow();
        assertThat(proposta.contaDestino()).isEqualTo("0000-000002");
        assertThat(proposta.nomeDestinatario()).isEqualTo("Bruno L.");
        // Preparar não move dinheiro.
        assertThat(transfers.count()).isZero();
        assertThat(transactions.findAll(origem.id())).hasSize(1);
        assertThat(transactions.findAll(destino.id())).isEmpty();
    }

    @Test
    void should_keepOnlyTheLastProposal_whenModelPreparesTwice() {
        depositar(origem, "100.00");

        tools.prepararTransferencia("0000000002", new BigDecimal("10.00"), null, contexto);
        tools.prepararTransferencia("0000000002", new BigDecimal("25.00"), null, contexto);

        assertThat(propostas.transferencia().orElseThrow().valor()).isEqualByComparingTo("25.00");
    }

    @Test
    void should_createNoProposal_whenThePreparationIsRefused() {
        depositar(origem, "10.00");

        assertThatThrownBy(() -> tools.prepararTransferencia("0000000002", new BigDecimal("50.00"), null, contexto))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Saldo insuficiente");
        assertThat(propostas.transferencia()).isEmpty();
    }

    @Test
    void should_failClearly_whenTheProposalContextIsMissing() {
        depositar(origem, "100.00");

        assertThatThrownBy(() -> tools.prepararTransferencia("0000000002", new BigDecimal("10.00"), null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_delegateBalanceToTheUseCase() {
        depositar(origem, "42.00");

        assertThat(tools.consultarSaldo().saldo()).isEqualByComparingTo("42.00");
    }

    private ToolCallback ferramenta(String nome) {
        return Arrays.stream(ToolCallbacks.from(tools))
                .filter(t -> t.getToolDefinition().name().equals(nome))
                .findFirst()
                .orElseThrow();
    }

    private Account novaConta(String nome, String numero) {
        var user = users.save(new User(new UserId(), nome.toLowerCase().replace(' ', '.') + "@example.com", nome, "hash"));
        return accounts.save(new Account(new AccountId(), user.id(), numero));
    }

    private void depositar(Account conta, String valor) {
        transactions.save(Transaction.nova(conta.id(), "Saldo inicial",
                new Money(new BigDecimal(valor).movePointRight(2).longValueExact()),
                Category.AUTO, TransactionType.RECEITA, LocalDate.of(2026, 11, 1)));
    }
}
