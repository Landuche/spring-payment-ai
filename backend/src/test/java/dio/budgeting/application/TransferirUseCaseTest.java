package dio.budgeting.application;

import dio.budgeting.domain.Account;
import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.BusinessRuleViolationException;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.LookupBlockedException;
import dio.budgeting.domain.NotFoundException;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionType;
import dio.budgeting.domain.User;
import dio.budgeting.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferirUseCaseTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);
    private final InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
    private final InMemoryTransferRepository transfers = new InMemoryTransferRepository();
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();

    private final Account origem = novaConta("Ana Souza", "0000000001");
    private final Account destino = novaConta("Bruno Lima", "0000000002");

    private final InMemoryLookupGuardRepository guards = new InMemoryLookupGuardRepository();

    private final InMemoryAuditEventRepository auditEvents = new InMemoryAuditEventRepository();
    private final AuditLog audit = new AuditLog(auditEvents, clock);

    private AccountId atual = origem.id();
    private final LookupGuardService lookupGuard =
            new LookupGuardService(guards, accounts, audit, clock);
    private final TransferirUseCase useCase = new TransferirUseCase(
            transfers, transactions, accounts, users, () -> atual, lookupGuard, audit, clock);

    @Test
    void should_auditRejectedTransfer_forInsufficientFunds() {
        depositar(origem, "10.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", new BigDecimal("50.00"), null, "sem-saldo"))
                .isInstanceOf(dio.budgeting.domain.BusinessRuleViolationException.class);

        assertThat(auditEvents.all()).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo(dio.budgeting.domain.AuditEventType.TRANSFERENCIA_RECUSADA);
            assertThat(e.reason()).isEqualTo("SALDO_INSUFICIENTE");
        });
        assertThat(transfers.count()).isZero();
    }

    @Test
    void should_blockAfterTenFailedLookups_evenForAValidDestination() {
        depositar(origem, "100.00");

        for (int i = 0; i < dio.budgeting.domain.LookupGuard.MAX_BUSCAS; i++) {
            assertThatThrownBy(() -> useCase.execute("9999999999", new BigDecimal("1.00"), null, "falha-" + UUID.randomUUID()))
                    .isInstanceOf(NotFoundException.class);
        }

        assertThatThrownBy(() -> useCase.execute("0000000002", new BigDecimal("1.00"), null, "chave-bloqueio"))
                .isInstanceOf(LookupBlockedException.class);
        assertThat(transfers.count()).isZero();
    }

    @Test
    void should_resetLookupCounter_afterASuccessfulTransfer() {
        depositar(origem, "100.00");
        for (int i = 0; i < 9; i++) {
            assertThatThrownBy(() -> useCase.execute("9999999999", new BigDecimal("1.00"), null, "falha-" + UUID.randomUUID()))
                    .isInstanceOf(NotFoundException.class);
        }

        useCase.execute("0000000002", new BigDecimal("1.00"), null, "ok-1");

        assertThat(guards.findByAccount(origem.id()).orElseThrow().attempts()).isZero();
    }

    @Test
    void should_debitOrigin_creditDestination_andLinkBothEntriesToTheTransfer() {
        depositar(origem, "500.00");

        var output = useCase.execute("0000000002", new BigDecimal("120.00"), "Almoço", "chave-1");

        assertThat(output.amount()).isEqualByComparingTo("120.00");
        assertThat(output.replayed()).isFalse();
        assertThat(saldo(origem.id())).isEqualByComparingTo("380.00");
        assertThat(saldo(destino.id())).isEqualByComparingTo("120.00");

        var lancamentosDaTransferencia = transactions.findAll(origem.id()).stream()
                .filter(t -> t.transferId() != null).toList();
        assertThat(lancamentosDaTransferencia).hasSize(1);
        assertThat(lancamentosDaTransferencia.get(0).type()).isEqualTo(TransactionType.TRANSFERENCIA_ENVIADA);
        assertThat(lancamentosDaTransferencia.get(0).category()).isEqualTo(Category.TRANSFERENCIA);
        assertThat(lancamentosDaTransferencia.get(0).description()).isEqualTo("Almoço");
    }

    @Test
    void should_rejectWhenBalanceIsInsufficient_withoutMovingAnything() {
        depositar(origem, "50.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", new BigDecimal("50.01"), null, "chave-2"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Saldo insuficiente");

        assertThat(saldo(origem.id())).isEqualByComparingTo("50.00");
        assertThat(saldo(destino.id())).isEqualByComparingTo("0");
        assertThat(transfers.count()).isZero();
    }

    @Test
    void should_replaySameIdempotencyKey_withoutDebitingAgain() {
        depositar(origem, "500.00");

        var first = useCase.execute("0000000002", new BigDecimal("100.00"), null, "chave-3");
        var second = useCase.execute("0000000002", new BigDecimal("100.00"), null, "chave-3");

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.replayed()).isTrue();
        assertThat(saldo(origem.id())).isEqualByComparingTo("400.00");
        assertThat(saldo(destino.id())).isEqualByComparingTo("100.00");
        assertThat(transfers.count()).isEqualTo(1);
    }

    @Test
    void should_rejectSelfTransfer() {
        depositar(origem, "100.00");

        assertThatThrownBy(() -> useCase.execute("0000000001", new BigDecimal("10.00"), null, "chave-4"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("própria conta");
    }

    @Test
    void should_return404_forUnknownDestination() {
        depositar(origem, "100.00");

        assertThatThrownBy(() -> useCase.execute("9999999999", new BigDecimal("10.00"), null, "chave-5"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_lockBothAccounts_inDeterministicOrder() {
        depositar(origem, "100.00");
        atual = destino.id();
        depositar(destino, "100.00");
        atual = origem.id();

        useCase.execute("0000000002", new BigDecimal("10.00"), null, "chave-6");

        var ordem = accounts.lockOrder();
        var esperada = java.util.stream.Stream.of(origem.id(), destino.id())
                .sorted(java.util.Comparator.comparing(id -> id.uuid().toString()))
                .toList();
        assertThat(ordem.subList(ordem.size() - 2, ordem.size())).containsExactlyElementsOf(esperada);
    }

    @Test
    void should_acceptFormattedAccountNumber_withDash() {
        depositar(origem, "100.00");

        var output = useCase.execute("0000-000002", new BigDecimal("10.00"), null, "chave-7");

        assertThat(output.replayed()).isFalse();
        assertThat(saldo(destino.id())).isEqualByComparingTo("10.00");
    }

    @Test
    void should_requireIdempotencyKey() {
        depositar(origem, "100.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", new BigDecimal("10.00"), null, " "))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void should_transferByEmail_ofTheRecipientOwner() {
        depositar(origem, "100.00");

        useCase.execute(null, "bruno.lima@example.com", new BigDecimal("15.00"), null, "chave-8");

        assertThat(saldo(destino.id())).isEqualByComparingTo("15.00");
    }

    @Test
    void should_useUserDescription_onBothStatementEntries() {
        depositar(origem, "100.00");

        useCase.execute("0000000002", null, new BigDecimal("10.00"), "Divisão do almoço", "chave-9");

        var enviado = transactions.findAll(origem.id()).stream().filter(t -> t.transferId() != null).findFirst().orElseThrow();
        var recebido = transactions.findAll(destino.id()).stream().filter(t -> t.transferId() != null).findFirst().orElseThrow();
        assertThat(enviado.description()).isEqualTo("Divisão do almoço");
        assertThat(recebido.description()).isEqualTo("Divisão do almoço");
    }

    @Test
    void should_requireExactlyOneDestination() {
        depositar(origem, "100.00");

        assertThatThrownBy(() -> useCase.execute(null, null, new BigDecimal("10.00"), null, "chave-10"))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> useCase.execute("0000000002", "bruno.lima@example.com", new BigDecimal("10.00"), null, "chave-11"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    private Account novaConta(String nome, String numero) {
        var user = users.save(new User(new UserId(), nome.toLowerCase().replace(' ', '.') + "@example.com", nome, "hash"));
        return accounts.save(new Account(new AccountId(), user.id(), numero));
    }

    private void depositar(Account conta, String valor) {
        var anterior = atual;
        atual = conta.id();
        // Entrada direta no extrato para preparar o cenário (o caso de uso de crédito é testado à parte).
        transactions.save(Transaction.nova(conta.id(), "Saldo inicial", new dio.budgeting.domain.Money(
                new BigDecimal(valor).movePointRight(2).longValueExact()), Category.AUTO, TransactionType.RECEITA,
                java.time.LocalDate.of(2026, 11, 1)));
        atual = anterior;
    }

    private BigDecimal saldo(AccountId conta) {
        long centavos = transactions.findAll(conta).stream()
                .mapToLong(t -> t.type().isCredit() ? t.amount().cents() : -t.amount().cents())
                .sum();
        return BigDecimal.valueOf(centavos, 2);
    }
}
