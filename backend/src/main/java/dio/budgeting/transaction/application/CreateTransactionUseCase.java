package dio.budgeting.transaction.application;

import dio.budgeting.account.application.CurrentAccountProvider;
import dio.budgeting.account.domain.AccountId;
import dio.budgeting.account.domain.AccountRepository;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionRepository;
import dio.budgeting.transaction.domain.TransactionType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class CreateTransactionUseCase {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CurrentAccountProvider currentAccount;
    private final Clock clock;

    public CreateTransactionUseCase(TransactionRepository transactionRepository, AccountRepository accountRepository,
                                    CurrentAccountProvider currentAccount, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.currentAccount = currentAccount;
        this.clock = clock;
    }

    @Transactional
    public TransactionOutput execute(BigDecimal amount, Category category, TransactionType type, String description) {

        var accountId = currentAccount.current();
        var money = Money.ofPositiveReais(amount);

        if (type.isExpense()) {
            // Trava a conta: duas despesas concorrentes não podem ler o mesmo saldo.
            accountRepository.lockForUpdate(accountId);
            var currentBalance = currentBalance(accountId);
            if (money.cents() > currentBalance.cents()) {
                throw new BusinessRuleViolationException(
                        "Saldo insuficiente: saldo atual de " + currentBalance.toReais()
                                + " reais não cobre a despesa de " + money.toReais() + " reais");
            }
        }

        var now = Instant.now(clock);
        var transaction = Transaction.create(accountId, description, money, category, type,
                LocalDate.ofInstant(now, clock.getZone()), now);
        return TransactionOutput.from(transactionRepository.save(transaction));
    }

    private Money currentBalance(AccountId accountId) {
        return transactionRepository.findAll(accountId).stream()
                .map(Transaction::signedAmount)
                .reduce(Money.ZERO, Money::plus);
    }
}
