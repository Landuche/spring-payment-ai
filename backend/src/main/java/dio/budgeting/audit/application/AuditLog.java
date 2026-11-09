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
 * Grava os eventos de auditoria. O sucesso entra na transação do chamador (existe só se o fato existiu), e a falha usa
 * uma transação própria, para continuar registrada mesmo quando a operação é desfeita.
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
    public void success(AuditEventType type, UserId actor, AccountId account, String reference) {
        events.save(AuditEvent.success(now(), type, actor, account, reference));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failure(AuditEventType type, UserId actor, AccountId account, String subjectHash,
                        String reference, String reason) {
        events.save(AuditEvent.failure(now(), type, actor, account, subjectHash, reference, reason));
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
