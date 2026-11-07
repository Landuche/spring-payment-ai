package dio.budgeting.auth.application;

import dio.budgeting.account.domain.AccountRepository;
import dio.budgeting.audit.application.AuditLog;
import dio.budgeting.audit.domain.AuditEventType;
import dio.budgeting.auth.domain.AuthenticationFailedException;
import dio.budgeting.auth.domain.RefreshToken;
import dio.budgeting.auth.domain.RefreshTokenRepository;
import dio.budgeting.auth.domain.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class RefreshSessionUseCase {
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final SessionIssuer sessionIssuer;
    private final AuditLog audit;
    private final Clock clock;

    public RefreshSessionUseCase(RefreshTokenRepository refreshTokens, UserRepository userRepository,
                                 AccountRepository accountRepository, SessionIssuer sessionIssuer, AuditLog audit,
                                 Clock clock) {
        this.refreshTokens = refreshTokens;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.sessionIssuer = sessionIssuer;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public SessionOutput execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationFailedException();
        }
        var now = Instant.now(clock);
        var current = refreshTokens.findByHashForUpdate(RefreshToken.hash(refreshToken))
                .orElseThrow(AuthenticationFailedException::new);

        if (current.isRevoked() || current.isExpired(now)) {
            throw new AuthenticationFailedException();
        }
        if (current.isUsed()) {
            // Token já trocado sendo apresentado de novo: cópia vazada ou replay. Encerra a sessão inteira.
            refreshTokens.revokeFamily(current.familyId(), now);
            audit.failure(AuditEventType.REFRESH_TOKEN_REUSE, current.userId(), null, null,
                    current.familyId().toString(), "TOKEN_ALREADY_USED");
            throw new AuthenticationFailedException();
        }

        refreshTokens.save(current.markAsUsed(now));

        var user = userRepository.findById(current.userId()).orElseThrow(AuthenticationFailedException::new);
        var account = accountRepository.findByOwner(user.id()).orElseThrow(AuthenticationFailedException::new);
        audit.success(AuditEventType.SESSION_RENEWED, user.id(), account.id(), current.familyId().toString());
        return sessionIssuer.issue(user, account, current.familyId());
    }
}
