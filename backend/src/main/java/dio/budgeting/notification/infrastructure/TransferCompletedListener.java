package dio.budgeting.notification.infrastructure;

import dio.budgeting.notification.application.CreateTransferNotificationsUseCase;
import dio.budgeting.transfer.domain.TransferCompleted;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
class TransferCompletedListener {
    private final JsonMapper jsonMapper;
    private final CreateTransferNotificationsUseCase createTransferNotifications;

    TransferCompletedListener(JsonMapper jsonMapper, CreateTransferNotificationsUseCase createTransferNotifications) {
        this.jsonMapper = jsonMapper;
        this.createTransferNotifications = createTransferNotifications;
    }

    @KafkaListener(topics = TransferCompleted.TOPIC)
    void onTransferCompleted(String payload) {
        createTransferNotifications.execute(parse(payload));
    }

    private TransferCompleted parse(String payload) {
        try {
            return jsonMapper.readValue(payload, TransferCompleted.class);
        } catch (JacksonException e) {
            throw new InvalidEventException("Evento " + TransferCompleted.TYPE + " ilegível", e);
        }
    }
}
