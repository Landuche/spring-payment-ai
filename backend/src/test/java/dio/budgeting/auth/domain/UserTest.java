package dio.budgeting.auth.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void should_maskAllButTheFirstName() {
        assertThat(user("Ana Paula Souza").maskedName()).isEqualTo("Ana P. S.");
    }

    @Test
    void should_keepASingleNameAsIs() {
        assertThat(user("Madonna").maskedName()).isEqualTo("Madonna");
    }

    @Test
    void should_ignoreExtraSpaces() {
        assertThat(user("  Bruno    Lima ").maskedName()).isEqualTo("Bruno L.");
    }

    private static User user(String name) {
        return new User(new UserId(), "ana@example.com", name, "hash");
    }
}
