package dio.budgeting.outbox.infrastructure;

import dio.budgeting.outbox.application.EventPublisher;
import dio.budgeting.outbox.application.EventPublishingException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
class KafkaEventPublisher implements EventPublisher {
    private static final long CONFIRMATION_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafka;

    KafkaEventPublisher(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(String topic, String key, String payload) {
        try {
            kafka.send(topic, key, payload).get(CONFIRMATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventPublishingException("Interrompido ao publicar em " + topic, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new EventPublishingException("Broker não confirmou a publicação em " + topic, e);
        }
    }
}
