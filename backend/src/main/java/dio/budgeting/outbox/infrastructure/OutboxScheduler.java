package dio.budgeting.outbox.infrastructure;

import dio.budgeting.outbox.application.PublishPendingEventsUseCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class OutboxScheduler {
    private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);

    private final PublishPendingEventsUseCase publishPendingEvents;

    OutboxScheduler(PublishPendingEventsUseCase publishPendingEvents) {
        this.publishPendingEvents = publishPendingEvents;
    }

    @Scheduled(fixedDelayString = "${budgeting.outbox.poll-interval-ms:1000}")
    void publishPending() {
        try {
            publishPendingEvents.execute();
        } catch (RuntimeException e) {
            log.error("Falha ao publicar eventos pendentes; o próximo ciclo tenta de novo", e);
        }
    }
}
