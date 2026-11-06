package dio.budgeting.application;

import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.AccountRepository;
import dio.budgeting.domain.AuditEventType;
import dio.budgeting.domain.LookupBlockedException;
import dio.budgeting.domain.LookupGuard;
import dio.budgeting.domain.LookupGuardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Limite de buscas de destinatário por conta: {@link LookupGuard#MAX_BUSCAS} buscas sem transferência concluída
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
    public void registrarBusca(AccountId accountId) {
        // Trava a conta: buscas concorrentes do mesmo usuário são contadas uma a uma.
        accountRepository.lockForUpdate(accountId);
        var now = Instant.now(clock);

        var conta = guardRepository.findByAccount(accountId).orElseGet(() -> LookupGuard.fresh(accountId));
        var proximaConta = conta.registrarBusca(now);
        guardRepository.save(proximaConta);
        if (proximaConta.isBlocked(now)) {
            audit.falha(AuditEventType.BUSCA_BLOQUEADA, null, accountId, null, null, "LIMITE_DE_BUSCAS");
            throw new LookupBlockedException(proximaConta.blockedUntil());
        }
    }

    /** Chamado dentro da transação da transferência concluída. */
    @Transactional
    public void limpar(AccountId accountId) {
        guardRepository.save(LookupGuard.fresh(accountId));
    }
}
