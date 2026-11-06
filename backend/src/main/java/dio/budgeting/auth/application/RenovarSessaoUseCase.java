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

/**
 * Troca um refresh token por um novo par (rotação). Reuso de token já trocado revoga a família inteira:
 * o revogamento é gravado mesmo com a resposta 401 (noRollbackFor), senão a proteção não teria efeito.
 */
@Service
public class RenovarSessaoUseCase {
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final SessionIssuer sessionIssuer;
    private final AuditLog audit;
    private final Clock clock;

    public RenovarSessaoUseCase(RefreshTokenRepository refreshTokens, UserRepository userRepository,
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
        var atual = refreshTokens.findByHashForUpdate(RefreshToken.hash(refreshToken))
                .orElseThrow(AuthenticationFailedException::new);

        if (atual.isRevoked() || atual.isExpired(now)) {
            throw new AuthenticationFailedException();
        }
        if (atual.isUsed()) {
            // Token já trocado sendo apresentado de novo: cópia vazada ou replay. Encerra a sessão inteira.
            refreshTokens.revokeFamily(atual.familyId(), now);
            audit.falha(AuditEventType.REUSO_REFRESH_TOKEN, atual.userId(), null, null,
                    atual.familyId().toString(), "TOKEN_JA_USADO");
            throw new AuthenticationFailedException();
        }

        refreshTokens.save(atual.marcarComoUsado(now));

        var user = userRepository.findById(atual.userId()).orElseThrow(AuthenticationFailedException::new);
        var account = accountRepository.findByOwner(user.id()).orElseThrow(AuthenticationFailedException::new);
        audit.sucesso(AuditEventType.SESSAO_RENOVADA, user.id(), account.id(), atual.familyId().toString());
        return sessionIssuer.emitir(user, account, atual.familyId());
    }
}
