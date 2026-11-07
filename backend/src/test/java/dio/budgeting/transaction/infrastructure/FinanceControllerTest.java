package dio.budgeting.transaction.infrastructure;

import dio.budgeting.account.infrastructure.SpringCurrentAccountProvider;
import dio.budgeting.auth.infrastructure.SecurityConfig;
import dio.budgeting.shared.infrastructure.GlobalExceptionHandler;
import dio.budgeting.transaction.application.GetBalanceUseCase;
import dio.budgeting.transaction.application.GenerateExpenseReportUseCase;
import dio.budgeting.transaction.application.InMemoryTransactionRepository;
import dio.budgeting.transaction.domain.TransactionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FinanceController.class,
        properties = "budgeting.security.jwt-secret=test-secret-com-pelo-menos-32-bytes-123456")
@AutoConfigureMockMvc
@Import({GetBalanceUseCase.class, GenerateExpenseReportUseCase.class,
        GlobalExceptionHandler.class, SecurityConfig.class, SpringCurrentAccountProvider.class,
        FinanceControllerTest.TestRepositoryConfig.class})
class FinanceControllerTest {

    private static final String ACCOUNT = "6f1d2c3a-1111-4a5b-8c9d-0e1f2a3b4c5d";

    @Autowired
    private MockMvc mvc;

    @Test
    void should_return_balance_and_expense_report_for_the_account() throws Exception {
        mvc.perform(get("/api/balance").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0));

        mvc.perform(get("/api/reports/expenses").param("period", "2026-10")
                        .with(jwt().jwt(j -> j.claim("accountId", ACCOUNT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("ALL"));
    }

    @Test
    void should_return_401_for_balance_without_token() throws Exception {
        mvc.perform(get("/api/balance")).andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class TestRepositoryConfig {
        @Bean
        TransactionRepository transactionRepository() {
            return new InMemoryTransactionRepository();
        }
    }
}
