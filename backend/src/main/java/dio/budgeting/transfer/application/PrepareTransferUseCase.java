package dio.budgeting.transfer.application;

import dio.budgeting.account.application.AccountOutput;
import dio.budgeting.account.application.GetAccountUseCase;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.application.GetBalanceUseCase;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Prepara uma transferência pedida ao assistente. NÃO move dinheiro: localiza o destinatário, confere saldo e conta
 * própria e devolve uma proposta. Quem executa é o cliente, tocando no botão de confirmação do app, pelo mesmo
 * endpoint de transferência (com chave de idempotência e todas as validações). Assim, nem a IA nem este caso de uso
 * decidem mover dinheiro.
 */
@Service
public class PrepareTransferUseCase {
    private final GetAccountUseCase getAccount;
    private final GetBalanceUseCase getBalance;

    public PrepareTransferUseCase(GetAccountUseCase getAccount, GetBalanceUseCase getBalance) {
        this.getAccount = getAccount;
        this.getBalance = getBalance;
    }

    /**
     * @param emailOrNumber e-mail do titular ou número da conta de destino
     * @param description    opcional
     */
    public TransferProposal execute(String emailOrNumber, BigDecimal amount, String description) {
        var money = Money.ofPositiveReais(amount);
        var recipient = locate(emailOrNumber);

        if (recipient.number().equals(getAccount.myAccount().number())) {
            throw new BusinessRuleViolationException("Não é possível transferir para a própria conta");
        }
        var balance = getBalance.execute().balance();
        if (money.toReais().compareTo(balance) > 0) {
            throw new BusinessRuleViolationException(
                    "Saldo insuficiente: saldo atual de " + balance + " reais não cobre a transferência de "
                            + money.toReais() + " reais");
        }

        var text = description == null || description.isBlank() ? null : description.strip();
        return new TransferProposal(recipient.number(), recipient.holderName(), money.toReais(), text);
    }

    private AccountOutput locate(String recipient) {
        var text = recipient == null ? "" : recipient.strip();
        return text.contains("@") ? getAccount.findByEmail(text) : getAccount.findByNumber(text);
    }
}
