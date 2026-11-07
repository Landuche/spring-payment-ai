package dio.budgeting.notification.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
class WebSocketConfig implements WebSocketConfigurer {
    private final NotificationSocketHandler handler;

    WebSocketConfig(NotificationSocketHandler handler) {
        this.handler = handler;
    }

    /** Mesmas origens do CORS: o app nativo não envia Origin, e o navegador só é aceito vindo de localhost. */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/notifications")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }
}
