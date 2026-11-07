package dio.budgeting.notification.infrastructure;

import dio.budgeting.auth.infrastructure.JwtTokenProvider;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationSocketHandlerTest {

    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
    private final SocketRegistry registry = spy(new SocketRegistry(Clock.systemUTC()));
    private final NotificationSocketHandler handler =
            new NotificationSocketHandler(jwtDecoder, registry, JsonMapper.builder().build());
    private final WebSocketSession session = mock(WebSocketSession.class);
    private final UUID account = UUID.randomUUID();

    NotificationSocketHandlerTest() {
        when(session.getId()).thenReturn("s1");
        when(session.isOpen()).thenReturn(true);
    }

    @Test
    void should_authenticateWithAValidToken_usingTheAccountAndExpirationOfTheToken() throws Exception {
        var expiresAt = Instant.now().plusSeconds(900);
        when(jwtDecoder.decode("token-valido")).thenReturn(Jwt.withTokenValue("token-valido")
                .header("alg", "HS256")
                .claim(JwtTokenProvider.ACCOUNT_CLAIM, account.toString())
                .issuedAt(Instant.now())
                .expiresAt(expiresAt)
                .build());
        handler.afterConnectionEstablished(session);

        handler.handleMessage(session, new TextMessage("{\"type\":\"auth\",\"token\":\"token-valido\"}"));

        verify(registry).authenticate(session, account, expiresAt);
        verify(session, never()).close(any(org.springframework.web.socket.CloseStatus.class));
    }

    @Test
    void should_closeWithUnauthorized_whenTheTokenIsInvalid() throws Exception {
        when(jwtDecoder.decode("token-ruim")).thenThrow(new BadJwtException("assinatura inválida"));
        handler.afterConnectionEstablished(session);

        handler.handleMessage(session, new TextMessage("{\"type\":\"auth\",\"token\":\"token-ruim\"}"));

        verify(session).close(SocketRegistry.UNAUTHORIZED);
        verify(registry, never()).authenticate(any(), any(), any());
    }

    @Test
    void should_closeWithUnauthorized_whenTheTokenHasNoAccountClaim() throws Exception {
        when(jwtDecoder.decode("sem-conta")).thenReturn(Jwt.withTokenValue("sem-conta")
                .header("alg", "HS256")
                .claim("sub", "alguem")
                .build());
        handler.afterConnectionEstablished(session);

        handler.handleMessage(session, new TextMessage("{\"type\":\"auth\",\"token\":\"sem-conta\"}"));

        verify(session).close(SocketRegistry.UNAUTHORIZED);
    }

    @Test
    void should_closeWithUnauthorized_whenTheFirstMessageIsNotAuth() throws Exception {
        handler.afterConnectionEstablished(session);

        handler.handleMessage(session, new TextMessage("{\"type\":\"ping\"}"));

        verify(session).close(SocketRegistry.UNAUTHORIZED);
        verify(jwtDecoder, never()).decode(any());
    }

    @Test
    void should_closeWithUnauthorized_whenTheMessageIsNotJson() throws Exception {
        handler.afterConnectionEstablished(session);

        handler.handleMessage(session, new TextMessage("isso não é json"));

        verify(session).close(eq(SocketRegistry.UNAUTHORIZED));
    }

    @Test
    void should_forgetTheConnection_whenItCloses() throws IOException {
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, org.springframework.web.socket.CloseStatus.NORMAL);

        verify(registry).remove(session);
    }
}
