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
public class RegistrarUsuarioUseCase {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_SENHA = 8;

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionIssuer sessionIssuer;
    private final AuditLog audit;

    public RegistrarUsuarioUseCase(UserRepository userRepository, AccountRepository accountRepository,
                                   PasswordEncoder passwordEncoder, SessionIssuer sessionIssuer, AuditLog audit) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionIssuer = sessionIssuer;
        this.audit = audit;
    }

    @Transactional
    public SessionOutput execute(String email, String name, String senha) {
        validar(email, senha);
        var normalizedEmail = email.strip().toLowerCase(Locale.ROOT);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("Já existe uma conta com este e-mail");
        }

        var user = userRepository.save(new User(new UserId(), normalizedEmail, name, passwordEncoder.encode(senha)));
        var account = accountRepository.save(Account.abrir(user.id()));
        audit.sucesso(AuditEventType.USUARIO_CADASTRADO, user.id(), account.id(), null);
        return sessionIssuer.emitir(user, account);
    }

    private static void validar(String email, String senha) {
        if (email == null || !EMAIL.matcher(email.strip()).matches()) {
            throw new BusinessRuleViolationException("E-mail inválido");
        }
        if (senha == null || senha.length() < MIN_SENHA) {
            throw new BusinessRuleViolationException("A senha deve ter pelo menos " + MIN_SENHA + " caracteres");
        }
    }
}
