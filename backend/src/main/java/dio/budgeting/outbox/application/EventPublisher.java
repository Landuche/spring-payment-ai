package dio.budgeting.outbox.application;

/** Envio ao broker. Só retorna quando o broker confirmou o recebimento, falha com {@link EventPublishingException}. */
public interface EventPublisher {
    void publish(String topic, String key, String payload);
}
