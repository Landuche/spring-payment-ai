package dio.budgeting.notification.infrastructure;

import dio.budgeting.IntegrationTest;
import dio.budgeting.account.domain.Account;
import dio.budgeting.account.domain.AccountRepository;
import dio.budgeting.auth.application.TokenProvider;
import dio.budgeting.auth.domain.User;
import dio.budgeting.auth.domain.UserId;
import dio.budgeting.auth.domain.UserRepository;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionRepository;
import dio.budgeting.transaction.domain.TransactionType;
import dio.budgeting.transfer.application.TransferUseCase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluxo completo do aviso em tempo real, com Postgres e Kafka reais e um cliente WebSocket de verdade: a transferência
 * grava os eventos no outbox, o Kafka os entrega, a notificação é criada e quem está conectado é avisado.
 */
@IntegrationTest
class NotificationPushFlowTest {

    @LocalServerPort private int port;
    @Autowired private UserRepository users;
    @Autowired private AccountRepository accounts;
    @Autowired private TransactionRepository transactions;
    @Autowired private TransferUseCase transfer;
    @Autowired private TokenProvider tokens;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_pushAChangeNotice_toTheReceiverConnectedByWebSocket() throws Exception {
        var sender = newAccount("Ana Souza");
        var receiver = newAccount("Bruno Lima");
        transactions.save(Transaction.create(sender.id(), "Saldo inicial", new Money(10_000), Category.AUTO,
                TransactionType.INCOME, LocalDate.now()));
        var inbox = new Inbox();
        var socket = inbox.connect(port);
        socket.sendMessage(new TextMessage(authMessage(tokenFor(receiver))));
        assertThat(inbox.next()).contains("ready");

        authenticateAs(sender);
        transfer.execute(receiver.number(), null, new BigDecimal("30.00"), null, UUID.randomUUID().toString());

        assertThat(inbox.next()).contains("notifications-changed");
        socket.close();
    }

    @Test
    void should_notWarnAnotherAccount() throws Exception {
        var sender = newAccount("Carla Dias");
        var receiver = newAccount("Davi Melo");
        var bystander = newAccount("Elisa Prado");
        transactions.save(Transaction.create(sender.id(), "Saldo inicial", new Money(10_000), Category.AUTO,
                TransactionType.INCOME, LocalDate.now()));
        var bystanderInbox = new Inbox();
        var bystanderSocket = bystanderInbox.connect(port);
        bystanderSocket.sendMessage(new TextMessage(authMessage(tokenFor(bystander))));
        assertThat(bystanderInbox.next()).contains("ready");
        var receiverInbox = new Inbox();
        var receiverSocket = receiverInbox.connect(port);
        receiverSocket.sendMessage(new TextMessage(authMessage(tokenFor(receiver))));
        assertThat(receiverInbox.next()).contains("ready");

        authenticateAs(sender);
        transfer.execute(receiver.number(), null, new BigDecimal("10.00"), null, UUID.randomUUID().toString());

        assertThat(receiverInbox.next()).contains("notifications-changed");
        assertThat(bystanderInbox.nextWithin(2)).isNull();
        bystanderSocket.close();
        receiverSocket.close();
    }

    @Test
    void should_closeTheConnection_whenTheTokenIsInvalid() throws Exception {
        var inbox = new Inbox();
        var socket = inbox.connect(port);

        socket.sendMessage(new TextMessage(authMessage("token-invalido")));

        assertThat(inbox.closedWith(10)).isEqualTo(4401);
    }

    private String tokenFor(Account account) {
        return tokens.issue(account.ownerId(), account.id()).value();
    }

    private static String authMessage(String token) {
        return "{\"type\":\"auth\",\"token\":\"" + token + "\"}";
    }

    private Account newAccount(String name) {
        var user = users.save(new User(new UserId(), UUID.randomUUID() + "@example.com", name, "hash"));
        return accounts.save(Account.open(user.id()));
    }

    private void authenticateAs(Account account) {
        var jwt = Jwt.withTokenValue("teste")
                .header("alg", "none")
                .claim("accountId", account.id().uuid().toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    /** Cliente WebSocket que guarda o que o servidor envia e o código com que a conexão foi fechada. */
    private static final class Inbox extends TextWebSocketHandler {
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final BlockingQueue<Integer> closes = new LinkedBlockingQueue<>();

        WebSocketSession connect(int port) throws Exception {
            return new StandardWebSocketClient()
                    .execute(this, "ws://localhost:" + port + "/ws/notifications")
                    .get(10, TimeUnit.SECONDS);
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            messages.add(message.getPayload());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            closes.add(status.getCode());
        }

        String next() throws InterruptedException {
            return nextWithin(30);
        }

        String nextWithin(int seconds) throws InterruptedException {
            return messages.poll(seconds, TimeUnit.SECONDS);
        }

        Integer closedWith(int seconds) throws InterruptedException {
            return closes.poll(seconds, TimeUnit.SECONDS);
        }
    }
}
