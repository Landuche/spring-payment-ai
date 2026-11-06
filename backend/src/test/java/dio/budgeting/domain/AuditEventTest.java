package dio.budgeting.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditEventTest {

    @Test
    void should_hashEmail_ignoringCaseAndSurroundingSpaces() {
        assertThat(AuditEvent.hashSubject(" Ana@Example.com "))
                .isEqualTo(AuditEvent.hashSubject("ana@example.com"));
    }

    @Test
    void should_neverExposeEmailInTheHash() {
        var hash = AuditEvent.hashSubject("ana@example.com");

        assertThat(hash).hasSize(64).doesNotContain("ana").doesNotContain("@");
    }

    @Test
    void should_rejectReasonLongerThanTheColumn() {
        assertThatThrownBy(() -> AuditEvent.falha(Instant.now(), AuditEventType.LOGIN_FALHOU, null, null, null,
                null, "x".repeat(61)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
