package dio.budgeting.auth.infrastructure;

import dio.budgeting.auth.domain.FailureCounter;
import dio.budgeting.auth.domain.FailureCounterRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class FailureCounterJpaRepository implements FailureCounterRepository {
    private final FailureCounterEntityRepository entityRepository;

    FailureCounterJpaRepository(FailureCounterEntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Override
    public Optional<FailureCounter> findByKey(String key) {
        return entityRepository.findById(key).map(FailureCounterEntity::toDomain);
    }

    @Override
    public Optional<FailureCounter> findByKeyForUpdate(String key) {
        return entityRepository.lockByKey(key).map(FailureCounterEntity::toDomain);
    }

    @Override
    public FailureCounter save(FailureCounter counter) {
        return entityRepository.save(FailureCounterEntity.from(counter)).toDomain();
    }
}
