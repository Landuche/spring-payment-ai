package dio.budgeting.assistant.infrastructure;

import dio.budgeting.account.application.CurrentAccountProvider;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

/**
 * monta o prompt do turno, aplica a memória e devolve a resposta com o que o app precisa.
 * <p>
 * A memória de conversa é isolada por conta: a chave é {@code conta:conversa}.
 */
@Service
public class AssistantChat {
    private static final String DEFAULT_CONVERSATION = "default";

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final Clock clock;
    private final CurrentAccountProvider currentAccount;

    public AssistantChat(ChatClient assistantChatClient,
                         @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                         Clock clock,
                         CurrentAccountProvider currentAccount) throws IOException {
        this.chatClient = assistantChatClient;
        this.systemPrompt = systemPrompt.getContentAsString(StandardCharsets.UTF_8);
        this.clock = clock;
        this.currentAccount = currentAccount;
    }

    public AssistantReplyResponse reply(String message, String conversationId) {
        String key = currentAccount.current().uuid() + ":"
                + (conversationId == null || conversationId.isBlank() ? DEFAULT_CONVERSATION : conversationId);
        var proposals = new TurnProposals();
        var text = chatClient.prompt()
                .system(systemPrompt + "\n\n" + dateContext(clock))
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, key))
                .toolContext(Map.of(AssistantTools.CONTEXT_PROPOSALS, proposals))
                .call()
                .content();
        return new AssistantReplyResponse(text, proposals.transfer().orElse(null));
    }

    static String dateContext(Clock clock) {
        var today = LocalDate.now(clock);
        var portuguese = Locale.of("pt", "BR");
        return "## Data de hoje\nHoje é %s, %s. O mês atual é %s de %d (período %s nas ferramentas)."
                .formatted(
                        today.getDayOfWeek().getDisplayName(TextStyle.FULL, portuguese),
                        today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        today.getMonth().getDisplayName(TextStyle.FULL, portuguese),
                        today.getYear(),
                        YearMonth.from(today));
    }
}
