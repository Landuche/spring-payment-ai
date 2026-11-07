package dio.budgeting.auth.application;

import dio.budgeting.account.domain.AccountRepository;
import dio.budgeting.audit.application.AuditLog;
import dio.budgeting.audit.domain.AuditEvent;
import dio.budgeting.audit.domain.AuditEventType;
import dio.budgeting.auth.domain.AuthenticationFailedException;
import dio.budgeting.auth.domain.LoginBlockedException;
import dio.budgeting.auth.domain.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class LoginUseCase {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionIssuer sessionIssuer;
    private final LoginProtectionService loginProtection;
    private final AuditLog audit;

    public LoginUseCase(UserRepository userRepository, AccountRepository accountRepository,
                        PasswordEncoder passwordEncoder, SessionIssuer sessionIssuer,
                        LoginProtectionService loginProtection, AuditLog audit) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionIssuer = sessionIssuer;
        this.loginProtection = loginProtection;
        this.audit = audit;
    }

    public SessionOutput execute(String email, String password) {
        var normalizedEmail = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        var emailKey = "email:" + normalizedEmail;
        var subject = AuditEvent.hashSubject(email);

        // conta bloqueada não recebe resposta de credencial.
        try {
            loginProtection.checkBlock(emailKey);
        } catch (LoginBlockedException e) {
            audit.failure(AuditEventType.LOGIN_BLOCKED, null, null, subject, null, "TOO_MANY_FAILURES");
            throw e;
        }

        if (email == null || password == null) {
            loginProtection.recordFailure(emailKey);
            audit.failure(AuditEventType.LOGIN_FAILED, null, null, subject, null, "MISSING_DATA");
            throw new AuthenticationFailedException();
        }

        var user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.passwordHash())) {
            loginProtection.recordFailure(emailKey);
            audit.failure(AuditEventType.LOGIN_FAILED, user == null ? null : user.id(), null, subject, null,
                    "INVALID_CREDENTIALS");
            throw new AuthenticationFailedException();
        }

        loginProtection.recordSuccess(emailKey);

        var account = accountRepository.findByOwner(user.id())
                .orElseThrow(() -> new IllegalStateException("Usuário sem conta: " + user.id()));
        audit.success(AuditEventType.LOGIN_SUCCEEDED, user.id(), account.id(), null);
        return sessionIssuer.issue(user, account);
    }
}
