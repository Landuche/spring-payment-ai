package dio.budgeting.transfer.application;

import dio.budgeting.account.application.AccountOutput;
import dio.budgeting.account.application.ConsultarContaUseCase;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.application.ConsultarSaldoUseCase;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Prepara uma transferência pedida ao assistente. NÃO move dinheiro: localiza o destinatário, confere saldo e conta
 * própria e devolve uma proposta. Quem executa é o cliente, tocando no botão de confirmação do app, pelo mesmo
 * endpoint de transferência (com chave de idempotência e todas as validações). Assim, nem a IA nem este caso de uso
 * decidem mover dinheiro.
 */
@Service
public class PrepararTransferenciaUseCase {
    private final ConsultarContaUseCase consultarConta;
    private final ConsultarSaldoUseCase consultarSaldo;

    public PrepararTransferenciaUseCase(ConsultarContaUseCase consultarConta, ConsultarSaldoUseCase consultarSaldo) {
        this.consultarConta = consultarConta;
        this.consultarSaldo = consultarSaldo;
    }

    /**
     * @param destinatario e-mail do titular ou número da conta de destino
     * @param descricao    opcional
     */
    public PropostaDeTransferencia execute(String destinatario, BigDecimal valor, String descricao) {
        var amount = Money.ofPositiveReais(valor);
        var recipient = localizar(destinatario);

        if (recipient.numero().equals(consultarConta.minhaConta().numero())) {
            throw new BusinessRuleViolationException("Não é possível transferir para a própria conta");
        }
        var saldo = consultarSaldo.execute().saldo();
        if (amount.toReais().compareTo(saldo) > 0) {
            throw new BusinessRuleViolationException(
                    "Saldo insuficiente: saldo atual de " + saldo + " reais não cobre a transferência de "
                            + amount.toReais() + " reais");
        }

        var texto = descricao == null || descricao.isBlank() ? null : descricao.strip();
        return new PropostaDeTransferencia(recipient.numero(), recipient.nomeTitular(), amount.toReais(), texto);
    }

    private AccountOutput localizar(String destinatario) {
        var texto = destinatario == null ? "" : destinatario.strip();
        return texto.contains("@") ? consultarConta.buscarPorEmail(texto) : consultarConta.buscarPorNumero(texto);
    }
}
