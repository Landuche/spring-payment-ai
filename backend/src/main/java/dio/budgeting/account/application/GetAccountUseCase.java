package dio.budgeting.account.application;

import dio.budgeting.account.domain.Account;
import dio.budgeting.account.domain.AccountRepository;
import dio.budgeting.auth.domain.User;
import dio.budgeting.auth.domain.UserRepository;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.NotFoundException;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class GetAccountUseCase {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern NON_DIGITS = Pattern.compile("\\D");
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("\\d{10}");

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CurrentAccountProvider currentAccount;
    private final LookupGuardService lookupGuard;

    public GetAccountUseCase(AccountRepository accountRepository, UserRepository userRepository,
                             CurrentAccountProvider currentAccount, LookupGuardService lookupGuard) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.currentAccount = currentAccount;
        this.lookupGuard = lookupGuard;
    }

    /** Dados da própria conta, incluindo o número que outras pessoas usam para transferir. */
    public AccountOutput myAccount() {
        var account = accountRepository.findById(currentAccount.current())
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        var owner = holder(account);
        return new AccountOutput(format(account.number()), owner.name());
    }

    /**
     * Confirmação antes de transferir. Devolve só o nome mascarado do titular,
     * para não expor dados pessoais a quem conhece apenas o número.
     */
    public AccountOutput findByNumber(String number) {
        var digits = number == null ? "" : NON_DIGITS.matcher(number).replaceAll("");
        if (!ACCOUNT_NUMBER.matcher(digits).matches()) {
            throw new BusinessRuleViolationException("Número de conta inválido");
        }
        lookupGuard.recordLookup(currentAccount.current());
        var account = accountRepository.findByNumber(digits)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        return new AccountOutput(format(account.number()), mask(holder(account).name()));
    }

    /**
     * Busca por e-mail (como uma chave Pix). Resposta idêntica para "não existe" e "bloqueado",
     * e nome mascarado: a enumeração de e-mails ainda precisa de limite de tentativas no gateway.
     */
    public AccountOutput findByEmail(String email) {
        if (email == null || !EMAIL.matcher(email).matches()) {
            throw new BusinessRuleViolationException("E-mail inválido");
        }
        lookupGuard.recordLookup(currentAccount.current());
        var user = userRepository.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        var account = accountRepository.findByOwner(user.id())
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));
        return new AccountOutput(format(account.number()), mask(user.name()));
    }

    private User holder(Account account) {
        return userRepository.findById(account.ownerId())
                .orElseThrow(() -> new IllegalStateException("Conta sem titular: " + account.id()));
    }

    static String format(String number) {
        return number.substring(0, 4) + "-" + number.substring(4);
    }

    /** "Ana Paula Souza" -> "Ana P. S." */
    static String mask(String name) {
        var parts = name.trim().split("\\s+");
        var sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(' ').append(parts[i].charAt(0)).append('.');
        }
        return sb.toString();
    }
}
