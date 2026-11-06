package dio.budgeting.audit.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.audit.domain.AuditEvent;
import dio.budgeting.audit.domain.AuditEventRepository;
import dio.budgeting.audit.domain.AuditEventType;
import dio.budgeting.auth.domain.UserId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Ponto único de gravação da trilha de auditoria. Duas regras de transação:
 * <ul>
 *   <li>{@link #sucesso}: entra na transação do caso de uso. Se a operação for revertida, o registro some junto:
 *       não existe "transferência concluída" que não aconteceu.</li>
 *   <li>{@link #falha}: grava em transação própria. Tentativas recusadas são justamente o que a auditoria precisa
 *       guardar, e elas acontecem em casos que sofrem rollback.</li>
 * </ul>
 */
@Service
public class AuditLog {
    private final AuditEventRepository events;
    private final Clock clock;

    public AuditLog(AuditEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void sucesso(AuditEventType type, UserId actor, AccountId account, String reference) {
        events.save(AuditEvent.sucesso(agora(), type, actor, account, reference));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void falha(AuditEventType type, UserId actor, AccountId account, String subjectHash,
                      String reference, String reason) {
        events.save(AuditEvent.falha(agora(), type, actor, account, subjectHash, reference, reason));
    }

    private Instant agora() {
        return Instant.now(clock);
    }
}
