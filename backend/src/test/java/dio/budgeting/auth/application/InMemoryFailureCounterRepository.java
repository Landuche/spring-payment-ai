package dio.budgeting.auth.application;

import dio.budgeting.auth.domain.FailureCounter;
import dio.budgeting.auth.domain.FailureCounterRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryFailureCounterRepository implements FailureCounterRepository {
    private final Map<String, FailureCounter> store = new HashMap<>();

    @Override
    public Optional<FailureCounter> findByKey(String key) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public Optional<FailureCounter> findByKeyForUpdate(String key) {
        return findByKey(key);
    }

    @Override
    public FailureCounter save(FailureCounter counter) {
        store.put(counter.key(), counter);
        return counter;
    }

    @Override
    public int deleteInactive(Instant cutoff) {
        var before = store.size();
        store.values().removeIf(c -> (c.blockedUntil() == null || c.blockedUntil().isBefore(cutoff))
                && (c.windowStart() == null || c.windowStart().isBefore(cutoff)));
        return before - store.size();
    }
}
