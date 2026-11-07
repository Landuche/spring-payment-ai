package dio.budgeting.notification.infrastructure;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationsCreatedListenerTest {

    private final SocketRegistry registry = mock(SocketRegistry.class);
    private final NotificationsCreatedListener listener =
            new NotificationsCreatedListener(JsonMapper.builder().build(), registry);

    @Test
    void should_notifyTheAccountNamedInTheEvent() {
        var account = UUID.randomUUID();

        listener.onNotificationsCreated("{\"accountId\":\"" + account + "\"}");

        verify(registry).notifyAccount(account);
    }

    @Test
    void should_rejectAnUnreadableEvent_withoutRetrying() {
        assertThatThrownBy(() -> listener.onNotificationsCreated("não é json"))
                .isInstanceOf(InvalidEventException.class);
    }
}
