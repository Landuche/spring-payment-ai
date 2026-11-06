package dio.budgeting.auth.infrastructure;

import dio.budgeting.auth.domain.FailureCounter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "failure_counter")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FailureCounterEntity {
    @Id
    @Column(name = "counter_key", length = 200)
    private String counterKey;

    @Column(nullable = false)
    private int failures;

    @Column(name = "window_start")
    private Instant windowStart;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    public static FailureCounterEntity from(FailureCounter counter) {
        return new FailureCounterEntity(counter.key(), counter.failures(), counter.windowStart(), counter.blockedUntil());
    }

    public FailureCounter toDomain() {
        return new FailureCounter(counterKey, failures, windowStart, blockedUntil);
    }
}
