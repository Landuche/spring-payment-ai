package dio.budgeting.domain;

import java.util.Optional;

public interface FailureCounterRepository {
    Optional<FailureCounter> findByKey(String key);

    /** Lê travando a linha: falhas concorrentes da mesma chave são contadas uma a uma. */
    Optional<FailureCounter> findByKeyForUpdate(String key);

    FailureCounter save(FailureCounter counter);
}
