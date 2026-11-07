package dio.budgeting.account.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.account.domain.AccountRepository;
import dio.budgeting.account.domain.LookupBlockedException;
import dio.budgeting.account.domain.LookupGuard;
import dio.budgeting.account.domain.LookupGuardRepository;
import dio.budgeting.audit.application.AuditLog;
import dio.budgeting.audit.domain.AuditEventType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Limite de buscas de destinatário por conta: {@link LookupGuard#MAX_LOOKUPS} buscas sem transferência concluída
 * geram bloqueio temporário. Limites por IP foram deliberadamente deixados de fora: em desenvolvimento e atrás de
 * proxy/Docker, todos os clientes compartilham o mesmo IP, o que bloquearia usuários legítimos.
 *
 * <p>Roda em transação própria (REQUIRES_NEW): a contagem persiste mesmo quando a busca falha e a transação do
 * chamador é revertida. Não contar tentativas falhas anularia a proteção.
 */
@Service
public class LookupGuardService {

    private final LookupGuardRepository guardRepository;
    private final AccountRepository accountRepository;
    private final AuditLog audit;
    private final Clock clock;

    public LookupGuardService(LookupGuardRepository guardRepository, AccountRepository accountRepository,
                              AuditLog audit, Clock clock) {
        this.guardRepository = guardRepository;
        this.accountRepository = accountRepository;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = LookupBlockedException.class)
    public void recordLookup(AccountId accountId) {
        // Trava a conta: buscas concorrentes do mesmo usuário são contadas uma a uma.
        accountRepository.lockForUpdate(accountId);
        var now = Instant.now(clock);

        var account = guardRepository.findByAccount(accountId).orElseGet(() -> LookupGuard.fresh(accountId));
        var updated = account.recordLookup(now);
        guardRepository.save(updated);
        if (updated.isBlocked(now)) {
            audit.failure(AuditEventType.LOOKUP_BLOCKED, null, accountId, null, null, "LOOKUP_LIMIT");
            throw new LookupBlockedException(updated.blockedUntil());
        }
    }

    /** Chamado dentro da transação da transferência concluída. */
    @Transactional
    public void reset(AccountId accountId) {
        guardRepository.save(LookupGuard.fresh(accountId));
    }
}
