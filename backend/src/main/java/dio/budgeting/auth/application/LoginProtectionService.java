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
 * Limite de tentativas de login por e-mail: após {@link #MAX_FALHAS_EMAIL} senhas erradas, a conta fica bloqueada
 * por {@link #BLOQUEIO}. Limite por IP foi removido pelo mesmo motivo dos limites de busca (IP compartilhado em dev e
 * atrás de proxy).
 */
@Service
public class LoginProtectionService {
    static final int MAX_FALHAS_EMAIL = 5;
    static final Duration JANELA = Duration.ofMinutes(15);
    static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private final FailureCounterRepository counters;
    private final Clock clock;

    public LoginProtectionService(FailureCounterRepository counters, Clock clock) {
        this.counters = counters;
        this.clock = clock;
    }

    /** Lança {@link LoginBlockedException} se o e-mail estiver bloqueado. Não altera estado. */
    public void verificarBloqueio(String emailKey) {
        var now = Instant.now(clock);
        var atual = counters.findByKey(emailKey).orElse(null);
        if (atual != null && atual.isBlocked(now)) {
            throw new LoginBlockedException(atual.blockedUntil());
        }
    }

    /** Registra uma falha, em transação própria: a contagem persiste mesmo que a transação do chamador seja revertida. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = LoginBlockedException.class)
    public void registrarFalha(String emailKey) {
        var now = Instant.now(clock);
        var atual = counters.findByKeyForUpdate(emailKey).orElseGet(() -> FailureCounter.fresh(emailKey));
        counters.save(atual.registrarFalha(now, MAX_FALHAS_EMAIL, JANELA, BLOQUEIO));
    }

    /** Login correto: zera o contador do e-mail. */
    @Transactional
    public void registrarSucesso(String emailKey) {
        counters.save(counters.findByKey(emailKey).orElseGet(() -> FailureCounter.fresh(emailKey)).limpar());
    }
}
