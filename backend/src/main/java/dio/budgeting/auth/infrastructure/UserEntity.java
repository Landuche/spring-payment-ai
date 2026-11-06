package dio.budgeting.auth.infrastructure;

import dio.budgeting.auth.domain.User;
import dio.budgeting.auth.domain.UserId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    public static UserEntity from(User user) {
        return new UserEntity(user.id().uuid(), user.email(), user.name(), user.passwordHash());
    }

    public User toDomain() {
        return new User(new UserId(id), email, name, passwordHash);
    }
}
