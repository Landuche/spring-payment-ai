package dio.budgeting.domain;

import java.util.UUID;

public record UserId(UUID uuid) {
    public UserId() {
        this(UUID.randomUUID());
    }
}
