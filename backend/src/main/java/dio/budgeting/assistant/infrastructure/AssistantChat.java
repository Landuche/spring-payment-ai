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
 * Conversa com o Claudinho: monta o prompt do turno, aplica a memória e devolve a resposta com o que o app precisa
 * mostrar além do texto (ex.: o cartão de transferência).
 * <p>
 * A memória de conversa é isolada por conta: a chave é {@code conta:conversa}, então um cliente nunca lê o histórico
 * de outra conta, mesmo que adivinhe o identificador da conversa.
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
        // As ferramentas deixam aqui o que o app precisa mostrar além do texto (ex.: o cartão de transferência).
        var proposals = new PropostasDoTurno();
        var text = chatClient.prompt()
                .system(systemPrompt + "\n\n" + contextoDeData(clock))
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, key))
                .toolContext(Map.of(AssistantTools.CONTEXT_PROPOSALS, proposals))
                .call()
                .content();
        return new AssistantReplyResponse(text, proposals.transferencia().orElse(null));
    }

    /**
     * O modelo não sabe que dia é hoje. Sem isso, "relatório deste mês" não tem como ser resolvido e ele acaba
     * perguntando o mês ao cliente.
     */
    static String contextoDeData(Clock clock) {
        var hoje = LocalDate.now(clock);
        var portugues = Locale.of("pt", "BR");
        return "## Data de hoje\nHoje é %s, %s. O mês atual é %s de %d (período %s nas ferramentas)."
                .formatted(
                        hoje.getDayOfWeek().getDisplayName(TextStyle.FULL, portugues),
                        hoje.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        hoje.getMonth().getDisplayName(TextStyle.FULL, portugues),
                        hoje.getYear(),
                        YearMonth.from(hoje));
    }
}
