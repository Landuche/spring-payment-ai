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
public class CriarTransacaoUseCase {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CurrentAccountProvider currentAccount;
    private final Clock clock;

    public CriarTransacaoUseCase(TransactionRepository transactionRepository, AccountRepository accountRepository,
                                 CurrentAccountProvider currentAccount, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.currentAccount = currentAccount;
        this.clock = clock;
    }

    @Transactional
    public TransactionOutput execute(BigDecimal valor, Category categoria, TransactionType tipo, String descricao) {

        var accountId = currentAccount.current();
        var amount = Money.ofPositiveReais(valor);

        if (!tipo.isCredit() && tipo.isExpense()) {
            // Trava a conta: duas despesas concorrentes não podem ler o mesmo saldo.
            accountRepository.lockForUpdate(accountId);
            var saldoAtual = saldoAtual(accountId);
            if (amount.cents() > saldoAtual.cents()) {
                throw new BusinessRuleViolationException(
                        "Saldo insuficiente: saldo atual de " + saldoAtual.toReais()
                                + " reais não cobre a despesa de " + amount.toReais() + " reais");
            }
        }

        var agora = Instant.now(clock);
        var transaction = Transaction.nova(accountId, descricao, amount, categoria, tipo,
                LocalDate.ofInstant(agora, clock.getZone()), agora);
        return TransactionOutput.from(transactionRepository.save(transaction));
    }

    private Money saldoAtual(AccountId accountId) {
        return transactionRepository.findAll(accountId).stream()
                .map(t -> t.type().isCredit() ? t.amount() : new Money(-t.amount().cents()))
                .reduce(Money.ZERO, Money::plus);
    }
}
