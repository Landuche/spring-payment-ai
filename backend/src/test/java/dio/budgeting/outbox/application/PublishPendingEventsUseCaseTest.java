package dio.budgeting.outbox.application;

import dio.budgeting.outbox.domain.OutboxEvent;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PublishPendingEventsUseCaseTest {

    private final Instant now = Instant.parse("2026-11-08T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final InMemoryOutboxRepository outbox = new InMemoryOutboxRepository();
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final PublishPendingEventsUseCase useCase = new PublishPendingEventsUseCase(outbox, publisher, clock);

    @Test
    void should_publishPendingEventsInOrder_andMarkThemAsPublished() {
        var first = pending("a", 1);
        var second = pending("b", 2);

        var published = useCase.execute();

        assertThat(published).isEqualTo(2);
        assertThat(publisher.payloads).containsExactly("a", "b");
        assertThat(outbox.get(first.id()).publishedAt()).isEqualTo(now);
        assertThat(outbox.get(second.id()).publishedAt()).isEqualTo(now);
    }

    @Test
    void should_useTheAggregateIdAsKey_soEventsOfTheSameTransferStayInOrder() {
        var event = pending("a", 1);

        useCase.execute();

        assertThat(publisher.keys).containsExactly(event.aggregateId().toString());
        assertThat(publisher.topics).containsExactly("transfer.completed");
    }

    @Test
    void should_notPublishAgain_whatWasAlreadyPublished() {
        pending("a", 1);
        useCase.execute();

        var secondRun = useCase.execute();

        assertThat(secondRun).isZero();
        assertThat(publisher.payloads).containsExactly("a");
    }

    @Test
    void should_stopAtTheFirstFailure_andKeepTheRestPending() {
        var first = pending("a", 1);
        var failing = pending("b", 2);
        var third = pending("c", 3);
        publisher.failOn.add("b");

        var published = useCase.execute();

        assertThat(published).isEqualTo(1);
        assertThat(outbox.get(first.id()).isPublished()).isTrue();
        assertThat(outbox.get(failing.id()).isPublished()).isFalse();
        assertThat(outbox.get(third.id()).isPublished()).isFalse();
    }

    @Test
    void should_retryTheFailedEventOnTheNextRun() {
        pending("a", 1);
        publisher.failOn.add("a");
        useCase.execute();

        publisher.failOn.clear();
        var published = useCase.execute();

        assertThat(published).isEqualTo(1);
        assertThat(publisher.payloads).containsExactly("a");
    }

    @Test
    void should_publishAtMostOneBatchPerRun() {
        for (int i = 0; i < PublishPendingEventsUseCase.BATCH_SIZE + 10; i++) {
            pending("event-" + i, i);
        }

        var published = useCase.execute();

        assertThat(published).isEqualTo(PublishPendingEventsUseCase.BATCH_SIZE);
    }

    private OutboxEvent pending(String payload, int secondsAfterStart) {
        var created = now.minusSeconds(1000).plusSeconds(secondsAfterStart);
        return outbox.save(OutboxEvent.pending("transfer.completed", UUID.randomUUID(), "TransferCompleted", payload, created));
    }

    private static final class RecordingPublisher implements EventPublisher {
        final List<String> topics = new ArrayList<>();
        final List<String> keys = new ArrayList<>();
        final List<String> payloads = new ArrayList<>();
        final Set<String> failOn = new HashSet<>();

        @Override
        public void publish(String topic, String key, String payload) {
            if (failOn.contains(payload)) {
                throw new EventPublishingException("broker indisponível", new RuntimeException());
            }
            topics.add(topic);
            keys.add(key);
            payloads.add(payload);
        }
    }
}
