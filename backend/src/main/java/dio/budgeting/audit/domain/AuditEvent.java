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
 * Registro imutável de eventos.
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

    public static AuditEvent success(Instant at, AuditEventType type, UserId actor, AccountId account,
                                     String reference) {
        return new AuditEvent(UUID.randomUUID(), at, type, AuditOutcome.SUCCESS, actor, account, null, reference, null);
    }

    public static AuditEvent failure(Instant at, AuditEventType type, UserId actor, AccountId account,
                                     String subjectHash, String reference, String reason) {
        return new AuditEvent(UUID.randomUUID(), at, type, AuditOutcome.FAILURE, actor, account, subjectHash,
                reference, reason);
    }

    /**
     * Guarda só o hash do e-mail, para contar tentativas sem armazenar o e-mail.
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
