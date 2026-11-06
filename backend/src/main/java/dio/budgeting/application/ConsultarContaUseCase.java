package dio.budgeting.application;

import dio.budgeting.application.output.AccountOutput;
import dio.budgeting.application.port.CurrentAccountProvider;
import dio.budgeting.domain.Account;
import dio.budgeting.domain.AccountRepository;
import dio.budgeting.domain.BusinessRuleViolationException;
import dio.budgeting.domain.NotFoundException;
import dio.budgeting.domain.User;
import dio.budgeting.domain.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ConsultarContaUseCase {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CurrentAccountProvider currentAccount;
    private final LookupGuardService lookupGuard;

    public ConsultarContaUseCase(AccountRepository accountRepository, UserRepository userRepository,
                                 CurrentAccountProvider currentAccount, LookupGuardService lookupGuard) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.currentAccount = currentAccount;
        this.lookupGuard = lookupGuard;
    }

    /** Dados da própria conta, incluindo o número que outras pessoas usam para transferir. */
    public AccountOutput minhaConta() {
        var account = accountRepository.findById(currentAccount.current())
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        var owner = titular(account);
        return new AccountOutput(formatar(account.number()), owner.name());
    }

    /**
     * Confirmação antes de transferir. Devolve só o nome mascarado do titular,
     * para não expor dados pessoais a quem conhece apenas o número.
     */
    public AccountOutput buscarPorNumero(String numero) {
        if (numero == null || !numero.replaceAll("\\D", "").matches("\\d{10}")) {
            throw new BusinessRuleViolationException("Número de conta inválido");
        }
        lookupGuard.registrarBusca(currentAccount.current());
        var account = accountRepository.findByNumber(numero.replaceAll("\\D", ""))
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        return new AccountOutput(formatar(account.number()), mascarar(titular(account).name()));
    }

    /**
     * Busca por e-mail (como uma chave Pix). Resposta idêntica para "não existe" e "bloqueado",
     * e nome mascarado: a enumeração de e-mails ainda precisa de limite de tentativas no gateway.
     */
    public AccountOutput buscarPorEmail(String email) {
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessRuleViolationException("E-mail inválido");
        }
        lookupGuard.registrarBusca(currentAccount.current());
        var user = userRepository.findByEmail(email.strip().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        var account = accountRepository.findByOwner(user.id())
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        return new AccountOutput(formatar(account.number()), mascarar(user.name()));
    }

    private User titular(Account account) {
        return userRepository.findById(account.ownerId())
                .orElseThrow(() -> new IllegalStateException("Conta sem titular: " + account.id()));
    }

    static String formatar(String numero) {
        return numero.substring(0, 4) + "-" + numero.substring(4);
    }

    /** "Ana Paula Souza" -> "Ana P. S." */
    static String mascarar(String nome) {
        var partes = nome.trim().split("\\s+");
        var sb = new StringBuilder(partes[0]);
        for (int i = 1; i < partes.length; i++) {
            sb.append(' ').append(partes[i].charAt(0)).append('.');
        }
        return sb.toString();
    }
}
