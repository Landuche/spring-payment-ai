package dio.budgeting.auth.application;

import dio.budgeting.auth.domain.FailureCounter;
import dio.budgeting.auth.domain.FailureCounterRepository;
import dio.budgeting.auth.domain.LoginBlockedException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Limite de tentativas de login por e-mail: após {@link #MAX_EMAIL_FAILURES} senhas erradas, a conta fica bloqueada
 * por {@link #BLOCK_DURATION}. Limite por IP foi removido pelo mesmo motivo dos limites de busca (IP compartilhado em dev e
 * atrás de proxy).
 */
@Service
public class LoginProtectionService {
    static final int MAX_EMAIL_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final FailureCounterRepository counters;
    private final Clock clock;

    public LoginProtectionService(FailureCounterRepository counters, Clock clock) {
        this.counters = counters;
        this.clock = clock;
    }

    /** Lança {@link LoginBlockedException} se o e-mail estiver bloqueado. Não altera estado. */
    public void checkBlock(String emailKey) {
        var now = Instant.now(clock);
        var current = counters.findByKey(emailKey).orElse(null);
        if (current != null && current.isBlocked(now)) {
            throw new LoginBlockedException(current.blockedUntil());
        }
    }

    /** Registra uma falha, em transação própria: a contagem persiste mesmo que a transação do chamador seja revertida. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = LoginBlockedException.class)
    public void recordFailure(String emailKey) {
        var now = Instant.now(clock);
        var current = counters.findByKeyForUpdate(emailKey).orElseGet(() -> FailureCounter.fresh(emailKey));
        counters.save(current.recordFailure(now, MAX_EMAIL_FAILURES, WINDOW, BLOCK_DURATION));
    }

    /** Login correto: zera o contador do e-mail. */
    @Transactional
    public void recordSuccess(String emailKey) {
        counters.save(counters.findByKey(emailKey).orElseGet(() -> FailureCounter.fresh(emailKey)).reset());
    }
}
