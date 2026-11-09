package dio.budgeting.transfer.infrastructure;

import dio.budgeting.IntegrationTest;
import dio.budgeting.account.domain.Account;
import dio.budgeting.account.domain.AccountRepository;
import dio.budgeting.auth.domain.User;
import dio.budgeting.auth.domain.UserId;
import dio.budgeting.auth.domain.UserRepository;
import dio.budgeting.notification.domain.NotificationRepository;
import dio.budgeting.notification.domain.NotificationType;
import dio.budgeting.outbox.application.OutboxRecorder;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionRepository;
import dio.budgeting.transaction.domain.TransactionType;
import dio.budgeting.transfer.application.TransferUseCase;
import dio.budgeting.transfer.domain.TransferCompleted;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Fluxo completo com Postgres e Kafka reais, a transferência grava o evento no outbox, o publicador o envia ao
 * Kafka, o consumidor lê e cria as notificações das duas contas.
 */
@IntegrationTest
class TransferNotificationFlowTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired private UserRepository users;
    @Autowired private AccountRepository accounts;
    @Autowired private TransactionRepository transactions;
    @Autowired private TransferUseCase transfer;
    @Autowired private NotificationRepository notifications;
    @Autowired private OutboxRecorder outbox;
    @Autowired private TransactionTemplate transactionTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_notifyBothAccounts_afterATransferThroughKafka() {
        var sender = newAccount("Ana Souza");
        var receiver = newAccount("Bruno Lima");
        transactions.save(Transaction.create(sender.id(), "Saldo inicial", new Money(10_000), Category.AUTO,
                TransactionType.INCOME, LocalDate.now()));
        authenticateAs(sender);

        transfer.execute(receiver.number(), null, new BigDecimal("30.00"), null, UUID.randomUUID().toString());

        await().atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(notifications.findLatest(sender.id(), 10)).singleElement().satisfies(n -> {
                assertThat(n.type()).isEqualTo(NotificationType.TRANSFER_SENT);
                assertThat(n.counterpartName()).isEqualTo("Bruno L.");
                assertThat(n.amount().cents()).isEqualTo(3000);
            });
            assertThat(notifications.findLatest(receiver.id(), 10)).singleElement().satisfies(n -> {
                assertThat(n.type()).isEqualTo(NotificationType.TRANSFER_RECEIVED);
                assertThat(n.counterpartName()).isEqualTo("Ana S.");
            });
        });
        assertThat(notifications.countUnread(sender.id())).isEqualTo(1);
        assertThat(notifications.countUnread(receiver.id())).isEqualTo(1);
    }

    @Test
    void should_createASingleNotificationPerAccount_whenTheSameEventIsDeliveredTwice() {
        var sender = newAccount("Carla Dias");
        var receiver = newAccount("Davi Melo");
        var event = new TransferCompleted(UUID.randomUUID(), sender.id().uuid(), receiver.id().uuid(), 500,
                "Carla D.", "Davi M.", Instant.now());

        record(event);
        record(event);

        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(notifications.findLatest(receiver.id(), 10)).hasSize(1));
        // Dá tempo para a segunda cópia ser consumida e descartada.
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(notifications.findLatest(sender.id(), 10)).hasSize(1);
            assertThat(notifications.findLatest(receiver.id(), 10)).hasSize(1);
        });
    }

    private void record(TransferCompleted event) {
        transactionTemplate.executeWithoutResult(status ->
                outbox.record(TransferCompleted.TOPIC, event.transferId(), TransferCompleted.TYPE, event));
    }

    private Account newAccount(String name) {
        var email = UUID.randomUUID() + "@example.com";
        var user = users.save(new User(new UserId(), email, name, "hash"));
        return accounts.save(Account.open(user.id()));
    }

    private void authenticateAs(Account account) {
        var jwt = Jwt.withTokenValue("teste")
                .header("alg", "none")
                .claim("accountId", account.id().uuid().toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
