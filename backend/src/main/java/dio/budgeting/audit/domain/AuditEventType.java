package dio.budgeting.audit.domain;

/** Catálogo fechado de eventos auditados. Adicionar um tipo é uma decisão de produto, não só de código. */
public enum AuditEventType {
    USER_REGISTERED,
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    LOGIN_BLOCKED,
    SESSION_RENEWED,
    REFRESH_TOKEN_REUSE,
    SESSION_ENDED,
    TRANSFER_COMPLETED,
    TRANSFER_REFUSED,
    LOOKUP_BLOCKED
}
