package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.FailureCounter;
import dio.budgeting.domain.FailureCounterRepository;
import dio.budgeting.infrastructure.persistence.entity.FailureCounterEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

interface FailureCounterEntityRepository extends CrudRepository<FailureCounterEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from FailureCounterEntity c where c.counterKey = :key")
    Optional<FailureCounterEntity> lockByKey(@Param("key") String key);

    @Modifying
    @Query("""
            delete from FailureCounterEntity c
            where (c.blockedUntil is null or c.blockedUntil < :corte)
              and (c.windowStart is null or c.windowStart < :corte)
            """)
    int apagarInativos(@Param("corte") Instant corte);
}

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
