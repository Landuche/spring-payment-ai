package dio.budgeting.notification.infrastructure;

import dio.budgeting.notification.domain.NotificationsCreated;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
class NotificationsCreatedListener {
    private final JsonMapper jsonMapper;
    private final SocketRegistry registry;

    NotificationsCreatedListener(JsonMapper jsonMapper, SocketRegistry registry) {
        this.jsonMapper = jsonMapper;
        this.registry = registry;
    }

    /**
     * Cada instância lê com um grupo só dela e, portanto, recebe todos os avisos: quem está conectado em qualquer
     * instância é avisado pela instância onde a conexão está. O grupo é novo a cada subida e começa do fim do
     * tópico, porque aviso antigo não interessa a quem acabou de conectar (o app lê a lista ao conectar).
     */
    @KafkaListener(topics = NotificationsCreated.TOPIC,
            groupId = "budgeting-push-#{T(java.util.UUID).randomUUID()}",
            properties = "auto.offset.reset=latest")
    void onNotificationsCreated(String payload) {
        registry.notifyAccount(parse(payload).accountId());
    }

    private NotificationsCreated parse(String payload) {
        try {
            return jsonMapper.readValue(payload, NotificationsCreated.class);
        } catch (JacksonException e) {
            throw new InvalidEventException("Evento " + NotificationsCreated.TYPE + " ilegível", e);
        }
    }
}
