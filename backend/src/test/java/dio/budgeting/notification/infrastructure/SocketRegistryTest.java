package dio.budgeting.notification.infrastructure;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocketRegistryTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-08T12:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };
    private final SocketRegistry registry = new SocketRegistry(clock);
    private final UUID account = UUID.randomUUID();
    private final UUID otherAccount = UUID.randomUUID();

    @Test
    void should_sendReady_whenTheConnectionAuthenticates() throws IOException {
        var session = session("s1");
        registry.open(session);

        registry.authenticate(session, account, now.get().plusSeconds(900));

        assertThat(textsSentTo(session)).containsExactly(SocketRegistry.READY);
    }

    @Test
    void should_notifyOnlyTheConnectionsOfThatAccount() throws IOException {
        var mine = session("mine");
        var other = session("other");
        registry.open(mine);
        registry.open(other);
        registry.authenticate(mine, account, now.get().plusSeconds(900));
        registry.authenticate(other, otherAccount, now.get().plusSeconds(900));

        registry.notifyAccount(account);

        assertThat(textsSentTo(mine)).containsExactly(SocketRegistry.READY, SocketRegistry.CHANGED);
        assertThat(textsSentTo(other)).containsExactly(SocketRegistry.READY);
    }

    @Test
    void should_notifyEveryDeviceOfTheSameAccount() throws IOException {
        var phone = session("phone");
        var tablet = session("tablet");
        registry.open(phone);
        registry.open(tablet);
        registry.authenticate(phone, account, now.get().plusSeconds(900));
        registry.authenticate(tablet, account, now.get().plusSeconds(900));

        registry.notifyAccount(account);

        assertThat(textsSentTo(phone)).contains(SocketRegistry.CHANGED);
        assertThat(textsSentTo(tablet)).contains(SocketRegistry.CHANGED);
    }

    @Test
    void should_notNotifyAnonymousConnections() throws IOException {
        var anonymous = session("anonymous");
        registry.open(anonymous);

        registry.notifyAccount(account);

        verify(anonymous, never()).sendMessage(any());
    }

    @Test
    void should_stopNotifying_afterTheConnectionIsRemoved() throws IOException {
        var session = session("s1");
        registry.open(session);
        registry.authenticate(session, account, now.get().plusSeconds(900));
        registry.remove(session);

        registry.notifyAccount(account);

        assertThat(textsSentTo(session)).containsExactly(SocketRegistry.READY);
        assertThat(registry.connectionCount()).isZero();
    }

    @Test
    void should_closeConnectionsThatDidNotAuthenticateInTime() throws IOException {
        var slow = session("slow");
        registry.open(slow);
        now.set(now.get().plus(SocketRegistry.AUTH_TIMEOUT).plusSeconds(1));

        registry.closeStaleConnections();

        verify(slow).close(SocketRegistry.UNAUTHORIZED);
        assertThat(registry.connectionCount()).isZero();
    }

    @Test
    void should_keepConnectionsStillWithinTheAuthenticationWindow() throws IOException {
        var recent = session("recent");
        registry.open(recent);
        now.set(now.get().plusSeconds(5));

        registry.closeStaleConnections();

        verify(recent, never()).close(any(CloseStatus.class));
        assertThat(registry.connectionCount()).isEqualTo(1);
    }

    @Test
    void should_closeConnectionsWhoseTokenExpired_soTheAppReconnectsWithAFreshOne() throws IOException {
        var session = session("s1");
        registry.open(session);
        registry.authenticate(session, account, now.get().plusSeconds(60));
        now.set(now.get().plusSeconds(61));

        registry.closeStaleConnections();

        verify(session).close(eq(SocketRegistry.TOKEN_EXPIRED));
        assertThat(registry.connectionCount()).isZero();
    }

    @Test
    void should_pingAuthenticatedConnections() throws IOException {
        var session = session("s1");
        registry.open(session);
        registry.authenticate(session, account, now.get().plusSeconds(900));

        registry.pingAll();

        var captor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(session, org.mockito.Mockito.atLeast(2)).sendMessage(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(message -> message instanceof PingMessage);
    }

    @Test
    void should_dropAConnectionThatFailsToReceive() throws IOException {
        var broken = session("broken");
        registry.open(broken);
        registry.authenticate(broken, account, now.get().plusSeconds(900));
        org.mockito.Mockito.doThrow(new IOException("fechada")).when(broken).sendMessage(any());

        registry.notifyAccount(account);

        assertThat(registry.connectionCount()).isZero();
    }

    private static WebSocketSession session(String id) {
        var session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private static List<String> textsSentTo(WebSocketSession session) throws IOException {
        var captor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(session, org.mockito.Mockito.atLeast(0)).sendMessage(captor.capture());
        return captor.getAllValues().stream()
                .filter(TextMessage.class::isInstance)
                .map(message -> ((TextMessage) message).getPayload())
                .toList();
    }
}
