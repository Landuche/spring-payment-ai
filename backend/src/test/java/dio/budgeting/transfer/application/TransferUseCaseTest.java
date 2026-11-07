package dio.budgeting.transfer.application;

import dio.budgeting.TestData;
import dio.budgeting.account.application.InMemoryAccountRepository;
import dio.budgeting.account.application.InMemoryLookupGuardRepository;
import dio.budgeting.account.application.LookupGuardService;
import dio.budgeting.account.domain.Account;
import dio.budgeting.account.domain.AccountId;
import dio.budgeting.account.domain.LookupBlockedException;
import dio.budgeting.audit.application.AuditLog;
import dio.budgeting.audit.application.InMemoryAuditEventRepository;
import dio.budgeting.auth.application.InMemoryUserRepository;
import dio.budgeting.outbox.application.InMemoryOutboxRecorder;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.NotFoundException;
import dio.budgeting.transaction.application.InMemoryTransactionRepository;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.TransactionType;
import dio.budgeting.transfer.domain.TransferCompleted;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferUseCaseTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);
    private final InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
    private final InMemoryTransferRepository transfers = new InMemoryTransferRepository();
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();

    private final Account source = TestData.newAccount(users, accounts, "Ana Souza", "0000000001");
    private final Account destination = TestData.newAccount(users, accounts, "Bruno Lima", "0000000002");

    private final InMemoryLookupGuardRepository guards = new InMemoryLookupGuardRepository();

    private final InMemoryAuditEventRepository auditEvents = new InMemoryAuditEventRepository();
    private final AuditLog audit = new AuditLog(auditEvents, clock);

    private AccountId current = source.id();
    private final LookupGuardService lookupGuard =
            new LookupGuardService(guards, accounts, audit, clock);
    private final InMemoryOutboxRecorder outbox = new InMemoryOutboxRecorder();
    private final TransferUseCase useCase = new TransferUseCase(
            transfers, transactions, accounts, users, () -> current, lookupGuard, audit, outbox, clock);

    @Test
    void should_recordATransferCompletedEvent_withMaskedNames() {
        TestData.deposit(transactions, source, "100.00");

        var output = useCase.execute("0000000002", null, new BigDecimal("30.00"), null, "evento-1");

        assertThat(outbox.all()).singleElement().satisfies(recorded -> {
            assertThat(recorded.topic()).isEqualTo(TransferCompleted.TOPIC);
            assertThat(recorded.eventType()).isEqualTo(TransferCompleted.TYPE);
            assertThat(recorded.aggregateId()).hasToString(output.id());
            var event = (TransferCompleted) recorded.payload();
            assertThat(event.transferId()).hasToString(output.id());
            assertThat(event.sourceAccountId()).isEqualTo(source.id().uuid());
            assertThat(event.destinationAccountId()).isEqualTo(destination.id().uuid());
            assertThat(event.amountCents()).isEqualTo(3000);
            assertThat(event.sourceName()).isEqualTo("Ana S.");
            assertThat(event.destinationName()).isEqualTo("Bruno L.");
        });
    }

    @Test
    void should_recordNoEvent_whenTheTransferIsRefused() {
        TestData.deposit(transactions, source, "10.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", null, new BigDecimal("50.00"), null, "evento-2"))
                .isInstanceOf(BusinessRuleViolationException.class);

        assertThat(outbox.all()).isEmpty();
    }

    @Test
    void should_recordTheEventOnlyOnce_whenTheSameKeyIsReplayed() {
        TestData.deposit(transactions, source, "100.00");

        useCase.execute("0000000002", null, new BigDecimal("30.00"), null, "evento-3");
        var replay = useCase.execute("0000000002", null, new BigDecimal("30.00"), null, "evento-3");

        assertThat(replay.replayed()).isTrue();
        assertThat(outbox.all()).hasSize(1);
    }

    @Test
    void should_auditRejectedTransfer_forInsufficientFunds() {
        TestData.deposit(transactions, source, "10.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", null, new BigDecimal("50.00"), null, "sem-saldo"))
                .isInstanceOf(dio.budgeting.shared.domain.BusinessRuleViolationException.class);

        assertThat(auditEvents.all()).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo(dio.budgeting.audit.domain.AuditEventType.TRANSFER_REFUSED);
            assertThat(e.reason()).isEqualTo("INSUFFICIENT_BALANCE");
        });
        assertThat(transfers.count()).isZero();
    }

    @Test
    void should_blockAfterTenFailedLookups_evenForAValidDestination() {
        TestData.deposit(transactions, source, "100.00");

        for (int i = 0; i < dio.budgeting.account.domain.LookupGuard.MAX_LOOKUPS; i++) {
            assertThatThrownBy(() -> useCase.execute("9999999999", null, new BigDecimal("1.00"), null, "falha-" + UUID.randomUUID()))
                    .isInstanceOf(NotFoundException.class);
        }

        assertThatThrownBy(() -> useCase.execute("0000000002", null, new BigDecimal("1.00"), null, "chave-bloqueio"))
                .isInstanceOf(LookupBlockedException.class);
        assertThat(transfers.count()).isZero();
    }

    @Test
    void should_resetLookupCounter_afterASuccessfulTransfer() {
        TestData.deposit(transactions, source, "100.00");
        for (int i = 0; i < 9; i++) {
            assertThatThrownBy(() -> useCase.execute("9999999999", null, new BigDecimal("1.00"), null, "falha-" + UUID.randomUUID()))
                    .isInstanceOf(NotFoundException.class);
        }

        useCase.execute("0000000002", null, new BigDecimal("1.00"), null, "ok-1");

        assertThat(guards.findByAccount(source.id()).orElseThrow().attempts()).isZero();
    }

    @Test
    void should_debitOrigin_creditDestination_andLinkBothEntriesToTheTransfer() {
        TestData.deposit(transactions, source, "500.00");

        var output = useCase.execute("0000000002", null, new BigDecimal("120.00"), "Almoço", "chave-1");

        assertThat(output.amount()).isEqualByComparingTo("120.00");
        assertThat(output.replayed()).isFalse();
        assertThat(balance(source.id())).isEqualByComparingTo("380.00");
        assertThat(balance(destination.id())).isEqualByComparingTo("120.00");

        var transferEntries = transactions.findAll(source.id()).stream()
                .filter(t -> t.transferId() != null).toList();
        assertThat(transferEntries).hasSize(1);
        assertThat(transferEntries.get(0).type()).isEqualTo(TransactionType.TRANSFER_SENT);
        assertThat(transferEntries.get(0).category()).isEqualTo(Category.TRANSFER);
        assertThat(transferEntries.get(0).description()).isEqualTo("Almoço");
    }

    @Test
    void should_rejectWhenBalanceIsInsufficient_withoutMovingAnything() {
        TestData.deposit(transactions, source, "50.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", null, new BigDecimal("50.01"), null, "chave-2"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Saldo insuficiente");

        assertThat(balance(source.id())).isEqualByComparingTo("50.00");
        assertThat(balance(destination.id())).isEqualByComparingTo("0");
        assertThat(transfers.count()).isZero();
    }

    @Test
    void should_replaySameIdempotencyKey_withoutDebitingAgain() {
        TestData.deposit(transactions, source, "500.00");

        var first = useCase.execute("0000000002", null, new BigDecimal("100.00"), null, "chave-3");
        var second = useCase.execute("0000000002", null, new BigDecimal("100.00"), null, "chave-3");

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.replayed()).isTrue();
        assertThat(balance(source.id())).isEqualByComparingTo("400.00");
        assertThat(balance(destination.id())).isEqualByComparingTo("100.00");
        assertThat(transfers.count()).isEqualTo(1);
    }

    @Test
    void should_rejectSelfTransfer() {
        TestData.deposit(transactions, source, "100.00");

        assertThatThrownBy(() -> useCase.execute("0000000001", null, new BigDecimal("10.00"), null, "chave-4"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("própria conta");
    }

    @Test
    void should_return404_forUnknownDestination() {
        TestData.deposit(transactions, source, "100.00");

        assertThatThrownBy(() -> useCase.execute("9999999999", null, new BigDecimal("10.00"), null, "chave-5"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_lockBothAccounts_inDeterministicOrder() {
        TestData.deposit(transactions, source, "100.00");
        current = destination.id();
        TestData.deposit(transactions, destination, "100.00");
        current = source.id();

        useCase.execute("0000000002", null, new BigDecimal("10.00"), null, "chave-6");

        var order = accounts.lockOrder();
        var expected = java.util.stream.Stream.of(source.id(), destination.id())
                .sorted(java.util.Comparator.comparing(id -> id.uuid().toString()))
                .toList();
        assertThat(order.subList(order.size() - 2, order.size())).containsExactlyElementsOf(expected);
    }

    @Test
    void should_acceptFormattedAccountNumber_withDash() {
        TestData.deposit(transactions, source, "100.00");

        var output = useCase.execute("0000-000002", null, new BigDecimal("10.00"), null, "chave-7");

        assertThat(output.replayed()).isFalse();
        assertThat(balance(destination.id())).isEqualByComparingTo("10.00");
    }

    @Test
    void should_requireIdempotencyKey() {
        TestData.deposit(transactions, source, "100.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", null, new BigDecimal("10.00"), null, " "))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void should_transferByEmail_ofTheRecipientOwner() {
        TestData.deposit(transactions, source, "100.00");

        useCase.execute(null, "bruno.lima@example.com", new BigDecimal("15.00"), null, "chave-8");

        assertThat(balance(destination.id())).isEqualByComparingTo("15.00");
    }

    @Test
    void should_useUserDescription_onBothStatementEntries() {
        TestData.deposit(transactions, source, "100.00");

        useCase.execute("0000000002", null, new BigDecimal("10.00"), "Divisão do almoço", "chave-9");

        var sent = transactions.findAll(source.id()).stream().filter(t -> t.transferId() != null).findFirst().orElseThrow();
        var received = transactions.findAll(destination.id()).stream().filter(t -> t.transferId() != null).findFirst().orElseThrow();
        assertThat(sent.description()).isEqualTo("Divisão do almoço");
        assertThat(received.description()).isEqualTo("Divisão do almoço");
    }

    @Test
    void should_requireExactlyOneDestination() {
        TestData.deposit(transactions, source, "100.00");

        assertThatThrownBy(() -> useCase.execute(null, null, new BigDecimal("10.00"), null, "chave-10"))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> useCase.execute("0000000002", "bruno.lima@example.com", new BigDecimal("10.00"), null, "chave-11"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    private BigDecimal balance(AccountId account) {
        long cents = transactions.findAll(account).stream()
                .mapToLong(t -> t.type().isCredit() ? t.amount().cents() : -t.amount().cents())
                .sum();
        return BigDecimal.valueOf(cents, 2);
    }
}
