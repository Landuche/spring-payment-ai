package dio.budgeting.auth.application;

import dio.budgeting.account.domain.Account;
import dio.budgeting.account.domain.AccountRepository;
import dio.budgeting.audit.application.AuditLog;
import dio.budgeting.audit.domain.AuditEventType;
import dio.budgeting.auth.domain.User;
import dio.budgeting.auth.domain.UserId;
import dio.budgeting.auth.domain.UserRepository;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.ConflictException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

/** Cria o usuário, abre a conta corrente e já devolve uma sessão. */
@Service
public class RegisterUserUseCase {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD = 8;

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionIssuer sessionIssuer;
    private final AuditLog audit;

    public RegisterUserUseCase(UserRepository userRepository, AccountRepository accountRepository,
                                   PasswordEncoder passwordEncoder, SessionIssuer sessionIssuer, AuditLog audit) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionIssuer = sessionIssuer;
        this.audit = audit;
    }

    @Transactional
    public SessionOutput execute(String email, String name, String password) {
        validate(email, password);
        var normalizedEmail = email.strip().toLowerCase(Locale.ROOT);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("Já existe uma conta com este e-mail");
        }

        var user = userRepository.save(new User(new UserId(), normalizedEmail, name, passwordEncoder.encode(password)));
        var account = accountRepository.save(Account.open(user.id()));
        audit.success(AuditEventType.USER_REGISTERED, user.id(), account.id(), null);
        return sessionIssuer.issue(user, account);
    }

    private static void validate(String email, String password) {
        if (email == null || !EMAIL.matcher(email.strip()).matches()) {
            throw new BusinessRuleViolationException("E-mail inválido");
        }
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new BusinessRuleViolationException("A senha deve ter pelo menos " + MIN_PASSWORD + " caracteres");
        }
    }
}
