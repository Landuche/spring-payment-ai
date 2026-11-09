package dio.budgeting.notification.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conexões WebSocket abertas desta instância. A conexão nasce anônima e só vira "autenticada" depois de a mensagem
 * de autenticação trazer um token válido, e quem não se autentica a tempo é fechado. O aviso só chega a conexões
 * autenticadas da conta certa.
 */
@Component
class SocketRegistry {
    static final CloseStatus UNAUTHORIZED = new CloseStatus(4401, "unauthorized");
    static final CloseStatus TOKEN_EXPIRED = new CloseStatus(4402, "token expired");
    static final Duration AUTH_TIMEOUT = Duration.ofSeconds(10);
    static final String READY = "{\"type\":\"ready\"}";
    static final String CHANGED = "{\"type\":\"notifications-changed\"}";

    private static final Logger log = LoggerFactory.getLogger(SocketRegistry.class);
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 64 * 1024;

    private record Pending(WebSocketSession session, Instant since) {
    }

    private record Authenticated(WebSocketSession session, UUID accountId, Instant expiresAt) {
    }

    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final Map<String, Authenticated> authenticated = new ConcurrentHashMap<>();
    private final Clock clock;

    SocketRegistry(Clock clock) {
        this.clock = clock;
    }

    void open(WebSocketSession session) {
        pending.put(session.getId(), new Pending(session, Instant.now(clock)));
    }

    void authenticate(WebSocketSession session, UUID accountId, Instant expiresAt) {
        pending.remove(session.getId());
        // Enviar de várias threads na mesma sessão não é seguro, então o decorador serializa os envios.
        var safeSession = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);
        var connection = new Authenticated(safeSession, accountId, expiresAt);
        authenticated.put(session.getId(), connection);
        send(connection, new TextMessage(READY));
    }

    void remove(WebSocketSession session) {
        pending.remove(session.getId());
        authenticated.remove(session.getId());
    }

    void notifyAccount(UUID accountId) {
        authenticated.values().stream()
                .filter(connection -> connection.accountId().equals(accountId))
                .forEach(connection -> send(connection, new TextMessage(CHANGED)));
    }

    /** Fecha quem não se autenticou a tempo e quem está com o token vencido (o app reconecta com um token novo). */
    void closeStaleConnections() {
        var now = Instant.now(clock);
        pending.values().stream()
                .filter(connection -> connection.since().plus(AUTH_TIMEOUT).isBefore(now))
                .forEach(connection -> close(connection.session(), UNAUTHORIZED));
        authenticated.values().stream()
                .filter(connection -> connection.expiresAt() != null && !connection.expiresAt().isAfter(now))
                .forEach(connection -> close(connection.session(), TOKEN_EXPIRED));
    }

    /** Mantém a conexão viva: proxies e túneis derrubam conexões ociosas depois de um tempo. */
    void pingAll() {
        authenticated.values().forEach(connection -> {
            try {
                connection.session().sendMessage(new PingMessage());
            } catch (IOException | RuntimeException e) {
                drop(connection, e);
            }
        });
    }

    private void send(Authenticated connection, TextMessage message) {
        try {
            connection.session().sendMessage(message);
        } catch (IOException | RuntimeException e) {
            drop(connection, e);
        }
    }

    private void drop(Authenticated connection, Exception cause) {
        log.debug("Conexão {} descartada: {}", connection.session().getId(), cause.getMessage());
        authenticated.remove(connection.session().getId());
        close(connection.session(), CloseStatus.SERVER_ERROR);
    }

    private void close(WebSocketSession session, CloseStatus status) {
        remove(session);
        try {
            session.close(status);
        } catch (IOException | RuntimeException e) {
            log.debug("Falha ao fechar a conexão {}: {}", session.getId(), e.getMessage());
        }
    }

    int connectionCount() {
        return pending.size() + authenticated.size();
    }
}
