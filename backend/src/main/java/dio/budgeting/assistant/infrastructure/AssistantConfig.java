package dio.budgeting.assistant.infrastructure;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AssistantConfig {

    /**
     * Cliente de chat do Claudinho: memória de conversa e ferramentas. O prompt de sistema não é fixo aqui porque
     * vai a cada chamada, junto com a data de hoje (ver {@link AssistantChat}).
     * Conversas curtas: as últimas 20 mensagens bastam para acompanhar o tom e o contexto.
     */
    @Bean
    ChatClient assistantChatClient(ChatClient.Builder builder, AssistantTools tools) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(tools)
                .build();
    }
}
