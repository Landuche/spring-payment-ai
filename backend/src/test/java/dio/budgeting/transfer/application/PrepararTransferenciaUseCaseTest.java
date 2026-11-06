package dio.budgeting.transfer.application;

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
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.shared.domain.NotFoundException;
import dio.budgeting.transaction.application.ConsultarSaldoUseCase;
import dio.budgeting.transaction.application.InMemoryTransactionRepository;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionType;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

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

    @Test
    void should_prepareProposalWithMaskedName_withoutMovingMoney() {
        depositar(origem, "100.00");

        var proposta = useCase.execute("0000000002", new BigDecimal("30.00"), "Pizza");

        assertThat(proposta.contaDestino()).isEqualTo("0000-000002");
        assertThat(proposta.nomeDestinatario()).isEqualTo("Bruno L.");
        assertThat(proposta.valor()).isEqualByComparingTo("30.00");
        assertThat(proposta.descricao()).isEqualTo("Pizza");
        // Preparar não move dinheiro: nenhuma transferência e nenhum lançamento novo.
        assertThat(transfers.count()).isZero();
        assertThat(transactions.findAll(origem.id())).hasSize(1);
        assertThat(transactions.findAll(destino.id())).isEmpty();
    }

    @Test
    void should_findRecipientByEmail() {
        depositar(origem, "100.00");

        var proposta = useCase.execute("bruno.lima@example.com", new BigDecimal("10.00"), null);

        assertThat(proposta.contaDestino()).isEqualTo("0000-000002");
    }

    @Test
    void should_acceptAccountNumberWithDash() {
        depositar(origem, "100.00");

        var proposta = useCase.execute("0000-000002", new BigDecimal("10.00"), null);

        assertThat(proposta.contaDestino()).isEqualTo("0000-000002");
    }

    @Test
    void should_dropBlankDescription() {
        depositar(origem, "100.00");

        var proposta = useCase.execute("0000000002", new BigDecimal("10.00"), "   ");

        assertThat(proposta.descricao()).isNull();
    }

    @Test
    void should_refuseWhenBalanceIsInsufficient() {
        depositar(origem, "10.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", new BigDecimal("50.00"), null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    void should_refuseTransferToOwnAccount() {
        depositar(origem, "100.00");

        assertThatThrownBy(() -> useCase.execute("0000000001", new BigDecimal("10.00"), null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("própria conta");
    }

    @Test
    void should_refuseUnknownRecipient() {
        depositar(origem, "100.00");

        assertThatThrownBy(() -> useCase.execute("9999999999", new BigDecimal("10.00"), null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_refuseNonPositiveAmount() {
        depositar(origem, "100.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", BigDecimal.ZERO, null))
                .isInstanceOf(BusinessRuleViolationException.class);
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
