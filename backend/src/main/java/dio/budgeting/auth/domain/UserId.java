package dio.budgeting.auth.domain;

import java.util.UUID;

public record UserId(UUID uuid) {
    public UserId() {
        this(UUID.randomUUID());
    }
}
