package dio.budgeting.application;

import dio.budgeting.domain.Account;
import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.BusinessRuleViolationException;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.NotFoundException;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionType;
import dio.budgeting.domain.User;
import dio.budgeting.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrepararTransferenciaUseCaseTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);
    private final InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
    private final InMemoryTransferRepository transfers = new InMemoryTransferRepository();
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryLookupGuardRepository guards = new InMemoryLookupGuardRepository();
    private final AuditLog audit = new AuditLog(new InMemoryAuditEventRepository(), clock);
    private final LookupGuardService lookupGuard = new LookupGuardService(guards, accounts, audit, clock);

    private final Account origem = novaConta("Ana Souza", "0000000001");
    private final Account destino = novaConta("Bruno Lima", "0000000002");

    private final PrepararTransferenciaUseCase useCase = new PrepararTransferenciaUseCase(
            new ConsultarContaUseCase(accounts, users, () -> origem.id(), lookupGuard),
            new ConsultarSaldoUseCase(transactions, () -> origem.id()));

    private final PropostasDoTurno propostas = new PropostasDoTurno();
    private final ToolContext contexto = new ToolContext(Map.of(PrepararTransferenciaUseCase.CONTEXTO, propostas));

    @Test
    void should_prepareProposalWithMaskedName_withoutMovingMoney() {
        depositar(origem, "100.00");

        var preparo = useCase.execute("0000000002", new BigDecimal("30.00"), "Pizza", contexto);

        var proposta = propostas.transferencia().orElseThrow();
        assertThat(proposta.contaDestino()).isEqualTo("0000-000002");
        assertThat(proposta.nomeDestinatario()).isEqualTo("Bruno L.");
        assertThat(proposta.valor()).isEqualByComparingTo("30.00");
        assertThat(proposta.descricao()).isEqualTo("Pizza");
        assertThat(preparo.aviso()).contains("AINDA NÃO");
        // Preparar não move dinheiro: nenhuma transferência e nenhum lançamento novo.
        assertThat(transfers.count()).isZero();
        assertThat(transactions.findAll(origem.id())).hasSize(1);
        assertThat(transactions.findAll(destino.id())).isEmpty();
    }

    @Test
    void should_hideTheContextFromTheModelSchema_andReceiveItWhenTheModelCallsTheTool() {
        depositar(origem, "100.00");

        // Mesmo mecanismo que o Spring AI usa para montar as ferramentas do ChatClient.
        var tool = ToolCallbacks.from(useCase)[0];

        assertThat(tool.getToolDefinition().name()).isEqualTo("prepararTransferencia");
        assertThat(tool.getToolDefinition().inputSchema())
                .contains("destinatario", "valor", "descricao")
                .doesNotContain("toolContext");

        var chamada = "{\"destinatario\":\"0000-000002\",\"valor\":30.00,\"descricao\":\"Pizza\"}";
        var resultado = tool.call(chamada, contexto);

        assertThat(resultado).contains("AGUARDANDO_CONFIRMACAO_DO_CLIENTE");
        assertThat(propostas.transferencia().orElseThrow().contaDestino()).isEqualTo("0000-000002");
        assertThat(transfers.count()).isZero();
    }

    @Test
    void should_findRecipientByEmail() {
        depositar(origem, "100.00");

        useCase.execute("bruno.lima@example.com", new BigDecimal("10.00"), null, contexto);

        assertThat(propostas.transferencia().orElseThrow().contaDestino()).isEqualTo("0000-000002");
    }

    @Test
    void should_acceptAccountNumberWithDash() {
        depositar(origem, "100.00");

        useCase.execute("0000-000002", new BigDecimal("10.00"), null, contexto);

        assertThat(propostas.transferencia()).isPresent();
    }

    @Test
    void should_keepOnlyTheLastProposal_whenModelPreparesTwice() {
        depositar(origem, "100.00");

        useCase.execute("0000000002", new BigDecimal("10.00"), null, contexto);
        useCase.execute("0000000002", new BigDecimal("25.00"), null, contexto);

        assertThat(propostas.transferencia().orElseThrow().valor()).isEqualByComparingTo("25.00");
    }

    @Test
    void should_refuseWhenBalanceIsInsufficient_andCreateNoProposal() {
        depositar(origem, "10.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", new BigDecimal("50.00"), null, contexto))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Saldo insuficiente");
        assertThat(propostas.transferencia()).isEmpty();
    }

    @Test
    void should_refuseTransferToOwnAccount() {
        depositar(origem, "100.00");

        assertThatThrownBy(() -> useCase.execute("0000000001", new BigDecimal("10.00"), null, contexto))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("própria conta");
        assertThat(propostas.transferencia()).isEmpty();
    }

    @Test
    void should_refuseUnknownRecipient_andCreateNoProposal() {
        depositar(origem, "100.00");

        assertThatThrownBy(() -> useCase.execute("9999999999", new BigDecimal("10.00"), null, contexto))
                .isInstanceOf(NotFoundException.class);
        assertThat(propostas.transferencia()).isEmpty();
    }

    @Test
    void should_refuseNonPositiveAmount() {
        depositar(origem, "100.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", BigDecimal.ZERO, null, contexto))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(propostas.transferencia()).isEmpty();
    }

    private Account novaConta(String nome, String numero) {
        var user = users.save(new User(new UserId(), nome.toLowerCase().replace(' ', '.') + "@example.com", nome, "hash"));
        return accounts.save(new Account(new AccountId(), user.id(), numero));
    }

    private void depositar(Account conta, String valor) {
        transactions.save(Transaction.nova(conta.id(), "Saldo inicial", new dio.budgeting.domain.Money(
                new BigDecimal(valor).movePointRight(2).longValueExact()), Category.AUTO, TransactionType.RECEITA,
                LocalDate.of(2026, 11, 1)));
    }
}
