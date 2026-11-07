package dio.budgeting.transfer.infrastructure;

import dio.budgeting.transfer.domain.TransferCompleted;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** O módulo que publica é dono do tópico: o Spring o cria no broker ao subir, se ainda não existir. */
@Configuration
class TransferTopics {

    /** Três partições para permitir mais de um consumidor; a chave (id da transferência) mantém a ordem por transferência. */
    @Bean
    NewTopic transferCompletedTopic() {
        return TopicBuilder.name(TransferCompleted.TOPIC).partitions(3).replicas(1).build();
    }
}
