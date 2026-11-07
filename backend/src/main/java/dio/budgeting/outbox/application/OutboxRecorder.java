package dio.budgeting.outbox.application;

import java.util.UUID;

/** Registra um evento de domínio para publicação. Só pode ser chamado dentro da transação do fato de negócio. */
public interface OutboxRecorder {
    void record(String topic, UUID aggregateId, String eventType, Object payload);
}
