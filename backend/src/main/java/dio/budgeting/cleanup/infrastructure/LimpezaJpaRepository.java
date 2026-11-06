package dio.budgeting.cleanup.infrastructure;

import dio.budgeting.account.infrastructure.LookupGuardEntityRepository;
import dio.budgeting.auth.infrastructure.FailureCounterEntityRepository;
import dio.budgeting.auth.infrastructure.RefreshTokenEntityRepository;
import dio.budgeting.cleanup.domain.LimpezaRepository;

import org.springframework.stereotype.Repository;

import java.time.Instant;

/** Delegação para as consultas de exclusão de cada tabela. Executado dentro da transação do caso de uso. */
@Repository
class LimpezaJpaRepository implements LimpezaRepository {
    private final FailureCounterEntityRepository failureCounters;
    private final LookupGuardEntityRepository lookupGuards;
    private final RefreshTokenEntityRepository refreshTokens;

    LimpezaJpaRepository(FailureCounterEntityRepository failureCounters, LookupGuardEntityRepository lookupGuards,
                         RefreshTokenEntityRepository refreshTokens) {
        this.failureCounters = failureCounters;
        this.lookupGuards = lookupGuards;
        this.refreshTokens = refreshTokens;
    }

    @Override
    public int apagarContadoresInativos(Instant corte) {
        return failureCounters.apagarInativos(corte);
    }

    @Override
    public int apagarBuscasSemPendencia(Instant agora) {
        return lookupGuards.apagarSemPendencia(agora);
    }

    @Override
    public int apagarRefreshTokensExpirados(Instant agora) {
        return refreshTokens.apagarExpirados(agora);
    }
}
