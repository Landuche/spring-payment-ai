package dio.budgeting.transaction.infrastructure;

import dio.budgeting.account.application.InMemoryAccountRepository;
import dio.budgeting.account.domain.AccountRepository;
import dio.budgeting.account.infrastructure.SpringCurrentAccountProvider;
import dio.budgeting.auth.infrastructure.SecurityConfig;
import dio.budgeting.shared.infrastructure.ClockConfig;
import dio.budgeting.shared.infrastructure.GlobalExceptionHandler;
import dio.budgeting.transaction.application.CriarTransacaoUseCase;
import dio.budgeting.transaction.application.InMemoryTransactionRepository;
import dio.budgeting.transaction.application.ListarTransacoesPorCategoriaUseCase;
import dio.budgeting.transaction.application.ListarTransacoesUseCase;
import dio.budgeting.transaction.domain.TransactionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class,
        properties = "budgeting.security.jwt-secret=test-secret-com-pelo-menos-32-bytes-123456")
@AutoConfigureMockMvc
@Import({CriarTransacaoUseCase.class, ListarTransacoesPorCategoriaUseCase.class, ListarTransacoesUseCase.class,
        GlobalExceptionHandler.class, ClockConfig.class, SecurityConfig.class, SpringCurrentAccountProvider.class,
        TransactionControllerTest.TestRepositoryConfig.class})
class TransactionControllerTest {

    private static final String CONTA = "6f1d2c3a-1111-4a5b-8c9d-0e1f2a3b4c5d";

    @Autowired
    private MockMvc mvc;

    @Test
    void should_create_and_list_transaction_for_the_authenticated_account() throws Exception {
        mvc.perform(post("/transactions").with(jwt().jwt(j -> j.claim("accountId", CONTA)))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"description":"Salário","category":"AUTO","type":"RECEITA","amount":500}
                        """))
                .andExpect(status().isCreated());

        mvc.perform(post("/transactions").with(jwt().jwt(j -> j.claim("accountId", CONTA)))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"description":"Mercado","category":"GROCERIES","type":"DESPESA","amount":45.90}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(45.9))
                .andExpect(jsonPath("$.type").value("DESPESA"));

        mvc.perform(get("/transactions/GROCERIES").with(jwt().jwt(j -> j.claim("accountId", CONTA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Mercado"));
    }

    @Test
    void should_return_401_without_token() throws Exception {
        mvc.perform(get("/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_400_with_field_errors_when_invalid() throws Exception {
        mvc.perform(post("/transactions").with(jwt().jwt(j -> j.claim("accountId", CONTA)))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"description":"","category":"GROCERIES","type":"DESPESA","amount":0}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.amount").exists());
    }

    @Test
    void should_return_422_when_expense_exceeds_balance() throws Exception {
        mvc.perform(post("/transactions").with(jwt().jwt(j -> j.claim("accountId", CONTA)))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"description":"Compra grande","category":"AUTO","type":"DESPESA","amount":999999}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("Saldo insuficiente")));
    }

    @Test
    void should_return_400_when_enum_is_unknown() throws Exception {
        mvc.perform(post("/transactions").with(jwt().jwt(j -> j.claim("accountId", CONTA)))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"description":"X","category":"INEXISTENTE","type":"DESPESA","amount":10}
                        """))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class TestRepositoryConfig {
        @Bean
        TransactionRepository transactionRepository() {
            return new InMemoryTransactionRepository();
        }

        @Bean
        AccountRepository accountRepository() {
            return new InMemoryAccountRepository();
        }
    }
}
