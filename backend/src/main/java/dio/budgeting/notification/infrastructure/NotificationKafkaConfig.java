package dio.budgeting.notification.infrastructure;

import dio.budgeting.notification.domain.NotificationsCreated;
import dio.budgeting.transfer.domain.TransferCompleted;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
class NotificationKafkaConfig {
    private static final String DEAD_LETTER_SUFFIX = ".DLT";
    private static final int PARTITIONS = 3;
    private static final long RETRY_INTERVAL_MS = 1000;
    private static final long RETRIES = 3;

    /**
     * Falha ao processar: tenta mais algumas vezes e, se continuar falhando, move a mensagem para a fila de falhas
     * do próprio tópico (.DLT) para não travar as seguintes. Mensagem ilegível pula as tentativas, porque tentar de
     * novo não a conserta.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafka) {
        var recoverer = new DeadLetterPublishingRecoverer(kafka, (record, error) ->
                new TopicPartition(record.topic() + DEAD_LETTER_SUFFIX, record.partition()));
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL_MS, RETRIES));
        handler.addNotRetryableExceptions(InvalidEventException.class);
        return handler;
    }

    /** Este módulo publica o aviso de notificação nova; a chave é a conta. */
    @Bean
    NewTopic notificationsCreatedTopic() {
        return topic(NotificationsCreated.TOPIC);
    }

    /** Mesmo número de partições do tópico original: a mensagem volta na mesma partição em que falhou. */
    @Bean
    NewTopic transferCompletedDeadLetterTopic() {
        return topic(TransferCompleted.TOPIC + DEAD_LETTER_SUFFIX);
    }

    @Bean
    NewTopic notificationsCreatedDeadLetterTopic() {
        return topic(NotificationsCreated.TOPIC + DEAD_LETTER_SUFFIX);
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(1).build();
    }
}
