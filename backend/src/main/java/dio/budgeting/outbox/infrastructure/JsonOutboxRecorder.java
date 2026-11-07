package dio.budgeting.outbox.infrastructure;

import dio.budgeting.outbox.application.OutboxRecorder;
import dio.budgeting.outbox.domain.OutboxEvent;
import dio.budgeting.outbox.domain.OutboxRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** MANDATORY: gravar o evento fora da transação do fato de negócio anularia o motivo de existir do outbox. */
@Component
class JsonOutboxRecorder implements OutboxRecorder {
    private final OutboxRepository outbox;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    JsonOutboxRecorder(OutboxRepository outbox, JsonMapper jsonMapper, Clock clock) {
        this.outbox = outbox;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String topic, UUID aggregateId, String eventType, Object payload) {
        outbox.save(OutboxEvent.pending(topic, aggregateId, eventType, jsonMapper.writeValueAsString(payload),
                Instant.now(clock)));
    }
}
