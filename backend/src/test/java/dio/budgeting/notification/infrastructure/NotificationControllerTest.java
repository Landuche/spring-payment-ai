package dio.budgeting.notification.infrastructure;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.account.infrastructure.SpringCurrentAccountProvider;
import dio.budgeting.auth.infrastructure.SecurityConfig;
import dio.budgeting.notification.application.InMemoryNotificationRepository;
import dio.budgeting.notification.application.ListNotificationsUseCase;
import dio.budgeting.notification.application.MarkNotificationsReadUseCase;
import dio.budgeting.notification.domain.Notification;
import dio.budgeting.notification.domain.NotificationType;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.shared.infrastructure.ClockConfig;
import dio.budgeting.shared.infrastructure.GlobalExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class,
        properties = "budgeting.security.jwt-secret=test-secret-com-pelo-menos-32-bytes-123456")
@AutoConfigureMockMvc
@Import({ListNotificationsUseCase.class, MarkNotificationsReadUseCase.class, ClockConfig.class,
        GlobalExceptionHandler.class, SecurityConfig.class, SpringCurrentAccountProvider.class,
        NotificationControllerTest.TestRepositoryConfig.class})
class NotificationControllerTest {

    private static final UUID ACCOUNT = UUID.fromString("6f1d2c3a-1111-4a5b-8c9d-0e1f2a3b4c5d");
    private static final UUID OTHER_ACCOUNT = UUID.fromString("7a2e3d4b-2222-4b6c-9d0e-1f2a3b4c5d6e");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private InMemoryNotificationRepository notifications;

    @BeforeEach
    void seed() {
        notifications.clear();
        notifications.saveIfAbsent(Notification.unread(UUID.randomUUID(), new AccountId(ACCOUNT),
                NotificationType.TRANSFER_RECEIVED, new Money(2550), "Bruno L.", Instant.parse("2026-11-08T12:00:00Z")));
        notifications.saveIfAbsent(Notification.unread(UUID.randomUUID(), new AccountId(OTHER_ACCOUNT),
                NotificationType.TRANSFER_SENT, new Money(100), "Ana S.", Instant.parse("2026-11-08T12:00:00Z")));
    }

    @Test
    void should_listTheNotificationsOfTheAuthenticatedAccount() throws Exception {
        mvc.perform(get("/api/notifications").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].type").value("TRANSFER_RECEIVED"))
                .andExpect(jsonPath("$.items[0].amount").value(25.5))
                .andExpect(jsonPath("$.items[0].counterpartName").value("Bruno L."))
                .andExpect(jsonPath("$.items[0].read").value(false));
    }

    @Test
    void should_markAsRead_andReturnNoContent() throws Exception {
        mvc.perform(post("/api/notifications/read").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT.toString()))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/notifications").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT.toString()))))
                .andExpect(jsonPath("$.unread").value(0))
                .andExpect(jsonPath("$.items[0].read").value(true));
    }

    @Test
    void should_notTouchTheNotificationsOfAnotherAccount() throws Exception {
        mvc.perform(post("/api/notifications/read").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT.toString()))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/notifications").with(jwt().jwt(j -> j.claim("accountId", OTHER_ACCOUNT.toString()))))
                .andExpect(jsonPath("$.unread").value(1));
    }

    @Test
    void should_return_401_without_token() throws Exception {
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/notifications/read")).andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class TestRepositoryConfig {
        @Bean
        InMemoryNotificationRepository notificationRepository() {
            return new InMemoryNotificationRepository();
        }
    }
}
