package dio.budgeting.application;

import dio.budgeting.domain.FailureCounter;
import dio.budgeting.domain.FailureCounterRepository;

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
}
