package dio.budgeting.auth.application;

import dio.budgeting.audit.application.AuditLog;
import dio.budgeting.audit.domain.AuditEventType;
import dio.budgeting.auth.domain.RefreshToken;
import dio.budgeting.auth.domain.RefreshTokenRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Logout: revoga a família do refresh token. Token desconhecido é ignorado sem revelar nada. */
@Service
public class EncerrarSessaoUseCase {
    private final RefreshTokenRepository refreshTokens;
    private final AuditLog audit;
    private final Clock clock;

    public EncerrarSessaoUseCase(RefreshTokenRepository refreshTokens, AuditLog audit, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public void execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokens.findByHash(RefreshToken.hash(refreshToken)).ifPresent(token -> {
            refreshTokens.revokeFamily(token.familyId(), Instant.now(clock));
            audit.sucesso(AuditEventType.SESSAO_ENCERRADA, token.userId(), null, token.familyId().toString());
        });
    }
}
