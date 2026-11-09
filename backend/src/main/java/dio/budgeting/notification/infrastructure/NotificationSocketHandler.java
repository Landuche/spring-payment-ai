package dio.budgeting.notification.infrastructure;

import dio.budgeting.auth.infrastructure.JwtTokenProvider;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.UUID;

/**
 * Canal de aviso em tempo real. O navegador e o app não conseguem mandar cabeçalho de autenticação ao abrir um
 * WebSocket de forma portátil, então a conexão abre anônima e o cliente se autentica na primeira mensagem:
 * {@code {"type":"auth","token":"<access token>"}}. O servidor só manda {@code notifications-changed}. O conteúdo
 * continua vindo da API REST.
 */
@Component
class NotificationSocketHandler extends TextWebSocketHandler {
    private static final String AUTH = "auth";

    private record ClientMessage(String type, String token) {
    }

    private final JwtDecoder jwtDecoder;
    private final SocketRegistry registry;
    private final JsonMapper jsonMapper;

    NotificationSocketHandler(JwtDecoder jwtDecoder, SocketRegistry registry, JsonMapper jsonMapper) {
        this.jwtDecoder = jwtDecoder;
        this.registry = registry;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        registry.open(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        try {
            var command = jsonMapper.readValue(message.getPayload(), ClientMessage.class);
            if (!AUTH.equals(command.type())) {
                reject(session);
                return;
            }
            Jwt jwt = jwtDecoder.decode(command.token());
            var accountId = UUID.fromString(jwt.getClaimAsString(JwtTokenProvider.ACCOUNT_CLAIM));
            registry.authenticate(session, accountId, jwt.getExpiresAt());
        } catch (JacksonException | JwtException | IllegalArgumentException | NullPointerException e) {
            reject(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        registry.remove(session);
    }

    private void reject(WebSocketSession session) throws IOException {
        registry.remove(session);
        session.close(SocketRegistry.UNAUTHORIZED);
    }
}
