package dio.budgeting.outbox.application;

import dio.budgeting.outbox.domain.OutboxRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Publica os eventos pendentes, do mais antigo ao mais novo. Entrega "pelo menos uma vez": se a aplicação cair
 * depois de o broker confirmar e antes de marcar o evento como publicado, ele é enviado de novo no próximo ciclo,
 * por isso os consumidores precisam ser idempotentes. Na primeira falha o lote para, para manter a ordem.
 */
@Service
public class PublishPendingEventsUseCase {
    static final int BATCH_SIZE = 50;

    private static final Logger log = LoggerFactory.getLogger(PublishPendingEventsUseCase.class);

    private final OutboxRepository outbox;
    private final EventPublisher publisher;
    private final Clock clock;

    public PublishPendingEventsUseCase(OutboxRepository outbox, EventPublisher publisher, Clock clock) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.clock = clock;
    }

    /** @return quantos eventos foram publicados neste ciclo */
    @Transactional
    public int execute() {
        var published = 0;
        for (var event : outbox.findPendingForUpdate(BATCH_SIZE)) {
            try {
                publisher.publish(event.topic(), event.aggregateId().toString(), event.payload());
            } catch (EventPublishingException e) {
                log.warn("Evento {} não publicado, tentando de novo no próximo ciclo: {}", event.id(), e.getMessage());
                break;
            }
            outbox.save(event.markPublished(Instant.now(clock)));
            published++;
        }
        return published;
    }
}
