package dio.budgeting.auth.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {
    private final SecurityConfig config = new SecurityConfig();

    @Test
    void should_useTheConfiguredSecret_when_itIsLongEnough() {
        var secret = "segredo-com-pelo-menos-32-bytes-123456";

        assertThat(config.jwtSecretKey(secret).getEncoded()).isEqualTo(secret.getBytes());
    }

    @Test
    void should_generateARandomKeyForEachStart_when_noSecretIsConfigured() {
        var first = config.jwtSecretKey("").getEncoded();
        var second = config.jwtSecretKey("  ").getEncoded();

        assertThat(first).hasSize(32);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void should_refuseAShortSecret() {
        assertThatThrownBy(() -> config.jwtSecretKey("curto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
