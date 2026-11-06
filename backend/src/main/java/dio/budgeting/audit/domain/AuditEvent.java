package dio.budgeting.audit.domain;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.auth.domain.UserId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro imutável de um evento sensível (login, sessão, transferência, bloqueios).
 *
 * <p>Minimização de dados: nunca guarda senha, token, nem e-mail em claro. Quando um e-mail precisa ser
 * correlacionado (tentativas de login de quem nem tem conta), guarda-se apenas o {@link #hashSubject(String) hash}.
 *
 * <p>Os identificadores são UUIDs simples, sem chave estrangeira: a trilha precisa existir mesmo quando as linhas
 * referenciadas mudam, e uma FK faria a gravação de falhas esperar locks da transação de negócio.
 */
public record AuditEvent(
        UUID id,
        Instant occurredAt,
        AuditEventType type,
        AuditOutcome outcome,
        UserId actor,
        AccountId account,
        String subjectHash,
        String reference,
        String reason) {

    public AuditEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(outcome, "outcome");
        requireAtMost(reference, 64, "reference");
        requireAtMost(reason, 60, "reason");
    }

    public static AuditEvent sucesso(Instant at, AuditEventType type, UserId actor, AccountId account,
                                     String reference) {
        return new AuditEvent(UUID.randomUUID(), at, type, AuditOutcome.SUCESSO, actor, account, null, reference, null);
    }

    public static AuditEvent falha(Instant at, AuditEventType type, UserId actor, AccountId account,
                                   String subjectHash, String reference, String reason) {
        return new AuditEvent(UUID.randomUUID(), at, type, AuditOutcome.FALHA, actor, account, subjectHash,
                reference, reason);
    }

    /**
     * Pseudônimo estável de um e-mail (SHA-256 do valor normalizado). Serve para correlacionar tentativas, não para
     * esconder o e-mail de quem tenta uma lista de candidatos: sem segredo, é pseudonimização, não criptografia.
     */
    public static String hashSubject(String email) {
        if (email == null) {
            return null;
        }
        var normalized = email.strip().toLowerCase(Locale.ROOT);
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }

    private static void requireAtMost(String value, int max, String field) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(field + " excede " + max + " caracteres");
        }
    }
}
