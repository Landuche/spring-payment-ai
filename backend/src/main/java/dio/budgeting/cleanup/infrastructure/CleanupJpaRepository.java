package dio.budgeting.cleanup.infrastructure;

import dio.budgeting.account.infrastructure.LookupGuardEntityRepository;
import dio.budgeting.auth.infrastructure.FailureCounterEntityRepository;
import dio.budgeting.auth.infrastructure.RefreshTokenEntityRepository;
import dio.budgeting.cleanup.domain.CleanupRepository;

import org.springframework.stereotype.Repository;

import java.time.Instant;

/** Delegação para as consultas de exclusão de cada tabela. Executado dentro da transação do caso de uso. */
@Repository
class CleanupJpaRepository implements CleanupRepository {
    private final FailureCounterEntityRepository failureCounters;
    private final LookupGuardEntityRepository lookupGuards;
    private final RefreshTokenEntityRepository refreshTokens;

    CleanupJpaRepository(FailureCounterEntityRepository failureCounters, LookupGuardEntityRepository lookupGuards,
                         RefreshTokenEntityRepository refreshTokens) {
        this.failureCounters = failureCounters;
        this.lookupGuards = lookupGuards;
        this.refreshTokens = refreshTokens;
    }

    @Override
    public int deleteInactiveCounters(Instant cutoff) {
        return failureCounters.deleteInactive(cutoff);
    }

    @Override
    public int deleteIdleLookups(Instant now) {
        return lookupGuards.deleteIdle(now);
    }

    @Override
    public int deleteExpiredRefreshTokens(Instant now) {
        return refreshTokens.deleteExpired(now);
    }
}
