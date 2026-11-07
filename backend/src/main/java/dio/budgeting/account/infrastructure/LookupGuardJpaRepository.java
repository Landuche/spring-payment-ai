package dio.budgeting.account.infrastructure;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.account.domain.LookupGuard;
import dio.budgeting.account.domain.LookupGuardRepository;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

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

    @Override
    public int deleteIdle(Instant now) {
        return entityRepository.deleteIdle(now);
    }
}
