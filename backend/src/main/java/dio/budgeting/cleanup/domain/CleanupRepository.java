package dio.budgeting.cleanup.domain;

import java.time.Instant;

/**
 * Remoção de registros que já não protegem nada. Cada método devolve quantas linhas apagou.
 * A auditoria (audit_event) fica de fora: é append-only e o banco recusa delete.
 */
public interface CleanupRepository {

    /** Contadores de falha sem bloqueio ativo e com janela anterior ao corte. */
    int deleteInactiveCounters(Instant cutoff);

    /** Contadores de busca sem busca acumulada e sem bloqueio ativo. */
    int deleteIdleLookups(Instant now);

    /** Refresh tokens expirados. Token expirado já é rejeitado antes da checagem de reuso, então apagá-lo não muda nada. */
    int deleteExpiredRefreshTokens(Instant now);
}
