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

    /**
     * Qualquer origem. O app nativo sempre envia o Origin do endereço público (o do túnel, por exemplo), que atrás de
     * um proxy não bate com o que o Spring enxerga. A checagem de origem protege conexões que se autenticam por
     * cookie; esta não usa cookie: nasce anônima e só vale depois de a primeira mensagem trazer o token de acesso.
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/notifications").setAllowedOriginPatterns("*");
    }
}
