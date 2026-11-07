package dio.budgeting.transfer.application;

import dio.budgeting.account.application.GetAccountUseCase;
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
import dio.budgeting.transaction.application.GetBalanceUseCase;
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

class PrepareTransferUseCaseTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);
    private final InMemoryTransactionRepository transactions = new InMemoryTransactionRepository();
    private final InMemoryTransferRepository transfers = new InMemoryTransferRepository();
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryLookupGuardRepository guards = new InMemoryLookupGuardRepository();
    private final AuditLog audit = new AuditLog(new InMemoryAuditEventRepository(), clock);
    private final LookupGuardService lookupGuard = new LookupGuardService(guards, accounts, audit, clock);

    private final Account source = newAccount("Ana Souza", "0000000001");
    private final Account destination = newAccount("Bruno Lima", "0000000002");

    private final PrepareTransferUseCase useCase = new PrepareTransferUseCase(
            new GetAccountUseCase(accounts, users, () -> source.id(), lookupGuard),
            new GetBalanceUseCase(transactions, () -> source.id()));

    @Test
    void should_prepareProposalWithMaskedName_withoutMovingMoney() {
        deposit(source, "100.00");

        var proposal = useCase.execute("0000000002", new BigDecimal("30.00"), "Pizza");

        assertThat(proposal.destinationAccount()).isEqualTo("0000-000002");
        assertThat(proposal.recipientName()).isEqualTo("Bruno L.");
        assertThat(proposal.amount()).isEqualByComparingTo("30.00");
        assertThat(proposal.description()).isEqualTo("Pizza");
        assertThat(transfers.count()).isZero();
        assertThat(transactions.findAll(source.id())).hasSize(1);
        assertThat(transactions.findAll(destination.id())).isEmpty();
    }

    @Test
    void should_findRecipientByEmail() {
        deposit(source, "100.00");

        var proposal = useCase.execute("bruno.lima@example.com", new BigDecimal("10.00"), null);

        assertThat(proposal.destinationAccount()).isEqualTo("0000-000002");
    }

    @Test
    void should_acceptAccountNumberWithDash() {
        deposit(source, "100.00");

        var proposal = useCase.execute("0000-000002", new BigDecimal("10.00"), null);

        assertThat(proposal.destinationAccount()).isEqualTo("0000-000002");
    }

    @Test
    void should_dropBlankDescription() {
        deposit(source, "100.00");

        var proposal = useCase.execute("0000000002", new BigDecimal("10.00"), "   ");

        assertThat(proposal.description()).isNull();
    }

    @Test
    void should_refuseWhenBalanceIsInsufficient() {
        deposit(source, "10.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", new BigDecimal("50.00"), null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    void should_refuseTransferToOwnAccount() {
        deposit(source, "100.00");

        assertThatThrownBy(() -> useCase.execute("0000000001", new BigDecimal("10.00"), null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("própria conta");
    }

    @Test
    void should_refuseUnknownRecipient() {
        deposit(source, "100.00");

        assertThatThrownBy(() -> useCase.execute("9999999999", new BigDecimal("10.00"), null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_refuseNonPositiveAmount() {
        deposit(source, "100.00");

        assertThatThrownBy(() -> useCase.execute("0000000002", BigDecimal.ZERO, null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    private Account newAccount(String name, String number) {
        var user = users.save(new User(new UserId(), name.toLowerCase().replace(' ', '.') + "@example.com", name, "hash"));
        return accounts.save(new Account(new AccountId(), user.id(), number));
    }

    private void deposit(Account account, String amount) {
        transactions.save(Transaction.create(account.id(), "Saldo inicial",
                new Money(new BigDecimal(amount).movePointRight(2).longValueExact()),
                Category.AUTO, TransactionType.INCOME, LocalDate.of(2026, 11, 1)));
    }
}
