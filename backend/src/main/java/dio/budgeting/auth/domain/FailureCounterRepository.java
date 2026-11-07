package dio.budgeting.auth.domain;

import java.time.Instant;
import java.util.Optional;

public interface FailureCounterRepository {
    Optional<FailureCounter> findByKey(String key);

    /** falhas concorrentes da mesma chave são contadas uma a uma. */
    Optional<FailureCounter> findByKeyForUpdate(String key);

    FailureCounter save(FailureCounter counter);

    /** Remove contadores sem bloqueio. */
    int deleteInactive(Instant cutoff);
}
