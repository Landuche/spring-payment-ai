package dio.budgeting.audit.domain;

/** Catálogo fechado de eventos auditados. Adicionar um tipo é uma decisão de produto, não só de código. */
public enum AuditEventType {
    USUARIO_CADASTRADO,
    LOGIN_REALIZADO,
    LOGIN_FALHOU,
    LOGIN_BLOQUEADO,
    SESSAO_RENOVADA,
    REUSO_REFRESH_TOKEN,
    SESSAO_ENCERRADA,
    TRANSFERENCIA_CONCLUIDA,
    TRANSFERENCIA_RECUSADA,
    BUSCA_BLOQUEADA
}
