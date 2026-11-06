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

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

    @Tool(name = "criarTransacao",
            description = "Registra uma nova receita ou despesa na data de hoje. Não serve para transferências entre contas")
    @Transactional
    public TransactionOutput execute(
            @ToolParam(description = "Valor em reais, maior que zero. Ex: 45.90") BigDecimal valor,
            @ToolParam(description = "Categoria da transação. GROCERIES: mercado, padaria, hortifruti. PHARMA: farmácia, remédios. "
                    + "AUTO: combustível, estacionamento, oficina, IPVA, carro. OUTROS: qualquer outro gasto, como adega, bar, restaurante, lazer. "
                    + "Use OUTROS quando nenhuma outra categoria se encaixar. Nunca recuse um gasto por causa da categoria.") Category categoria,
            @ToolParam(description = "Tipo da transação: RECEITA ou DESPESA") TransactionType tipo,
            @ToolParam(description = "Descrição curta do gasto ou da receita. Opcional", required = false)
            String descricao) {

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
