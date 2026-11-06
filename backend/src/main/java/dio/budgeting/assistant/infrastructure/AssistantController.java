package dio.budgeting.assistant.infrastructure;

import dio.budgeting.account.application.CurrentAccountProvider;
import dio.budgeting.assistant.application.PrepararTransferenciaUseCase;
import dio.budgeting.assistant.application.PropostasDoTurno;
import dio.budgeting.investment.application.SimularInvestimentoUseCase;
import dio.budgeting.transaction.application.ConsultarSaldoUseCase;
import dio.budgeting.transaction.application.CriarTransacaoUseCase;
import dio.budgeting.transaction.application.GerarRelatorioGastosUseCase;

import jakarta.validation.Valid;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
 * Porta de entrada do assistente (Claudinho). A IA só executa operações através dos casos de uso
 * registrados como tools; nunca acessa o repositório diretamente.
 * <p>
 * A memória de conversa é isolada por conta: a chave é {@code conta:conversa}, então um cliente nunca
 * lê o histórico de outra conta, mesmo que adivinhe o identificador da conversa.
 * <p>
 * A resposta falada fica por conta do app (voz do dispositivo); o backend só transcreve e responde em texto.
 */
@RestController
@RequestMapping("/assistant")
public class AssistantController {
    private static final String CONVERSA_PADRAO = "default";

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final Clock clock;
    private final TranscriptionModel transcriptionModel;
    private final CurrentAccountProvider currentAccount;

    public AssistantController(ChatClient.Builder chatClientBuilder,
                               @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                               Clock clock,
                               TranscriptionModel transcriptionModel,
                               CurrentAccountProvider currentAccount,
                               CriarTransacaoUseCase criarTransacao,
                               ConsultarSaldoUseCase consultarSaldo,
                               GerarRelatorioGastosUseCase gerarRelatorioGastos,
                               SimularInvestimentoUseCase simularInvestimento,
                               PrepararTransferenciaUseCase prepararTransferencia) throws IOException {
        this.systemPrompt = systemPrompt.getContentAsString(StandardCharsets.UTF_8);
        this.clock = clock;
        this.transcriptionModel = transcriptionModel;
        this.currentAccount = currentAccount;

        // O prompt de sistema é enviado a cada chamada (com a data de hoje), então não há defaultSystem.
        // Conversas curtas: as últimas 20 mensagens bastam para acompanhar o tom e o contexto.
        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(criarTransacao, consultarSaldo, gerarRelatorioGastos, simularInvestimento,
                        prepararTransferencia)
                .build();
    }

    @PostMapping(value = "/text", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AssistantReplyResponse text(@Valid @RequestBody AssistantCommandRequest request) {
        return responder(request.message(), request.conversationId());
    }

    /**
     * O app avisa o desfecho do cartão de transferência (enviada, cancelada ou com falha) e o Claudinho responde na
     * mesma conversa, com memória, propondo o próximo passo. Não executa nada: a transferência já foi tratada pelo app.
     */
    @PostMapping(value = "/event", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AssistantReplyResponse event(@Valid @RequestBody AssistantEventRequest request) {
        return responder(AvisosDoApp.mensagem(request), request.conversationId());
    }

    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public VoiceReplyResponse voice(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "conversationId", required = false) String conversationId) {
        var transcription = transcriptionModel.transcribe(file.getResource());
        var resposta = responder(transcription, conversationId);
        return new VoiceReplyResponse(transcription, resposta.reply(), resposta.transferencia());
    }

    private AssistantReplyResponse responder(String mensagem, String conversationId) {
        String chave = currentAccount.current().uuid() + ":"
                + (conversationId == null || conversationId.isBlank() ? CONVERSA_PADRAO : conversationId);
        // As ferramentas deixam aqui o que o app precisa mostrar além do texto (ex.: o cartão de transferência).
        var propostas = new PropostasDoTurno();
        var texto = chatClient.prompt()
                .system(systemPrompt + "\n\n" + contextoDeData(clock))
                .user(mensagem)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chave))
                .toolContext(Map.of(PrepararTransferenciaUseCase.CONTEXTO, propostas))
                .call()
                .content();
        return new AssistantReplyResponse(texto, propostas.transferencia().orElse(null));
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
