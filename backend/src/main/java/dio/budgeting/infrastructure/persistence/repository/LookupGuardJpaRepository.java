package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.LookupGuard;
import dio.budgeting.domain.LookupGuardRepository;
import dio.budgeting.infrastructure.persistence.entity.LookupGuardEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface LookupGuardEntityRepository extends CrudRepository<LookupGuardEntity, UUID> {

    /** Só remove contadores zerados e sem bloqueio: sem eles, a linha equivale a um registro novo. */
    @Modifying
    @Query("""
            delete from LookupGuardEntity g
            where g.attempts = 0
              and (g.blockedUntil is null or g.blockedUntil < :agora)
            """)
    int apagarSemPendencia(@Param("agora") Instant agora);
}

@Repository
class LookupGuardJpaRepository implements LookupGuardRepository {
    private final LookupGuardEntityRepository entityRepository;

    LookupGuardJpaRepository(LookupGuardEntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Override
    public Optional<LookupGuard> findByAccount(AccountId accountId) {
        return entityRepository.findById(accountId.uuid()).map(LookupGuardEntity::toDomain);
    }

    @Override
    public LookupGuard save(LookupGuard guard) {
        return entityRepository.save(LookupGuardEntity.from(guard)).toDomain();
    }
}
