package dio.budgeting.investment.infrastructure;

import dio.budgeting.account.infrastructure.SpringCurrentAccountProvider;
import dio.budgeting.auth.infrastructure.SecurityConfig;
import dio.budgeting.investment.application.SimulateInvestmentUseCase;
import dio.budgeting.shared.infrastructure.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InvestmentController.class,
        properties = {"budgeting.investment.monthly-rate=0.01",
                "budgeting.security.jwt-secret=test-secret-com-pelo-menos-32-bytes-123456"})
@AutoConfigureMockMvc
@Import({SimulateInvestmentUseCase.class, GlobalExceptionHandler.class, SecurityConfig.class,
        SpringCurrentAccountProvider.class})
class InvestmentControllerTest {

    private static final String ACCOUNT = "6f1d2c3a-1111-4a5b-8c9d-0e1f2a3b4c5d";

    @Autowired
    private MockMvc mvc;

    @Test
    void should_return_simulation_with_compound_interest() throws Exception {
        mvc.perform(post("/api/simulations").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT)))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"amount":1000,"months":12}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalAmount").value(1126.83));
    }

    @Test
    void should_reject_simulation_out_of_range() throws Exception {
        mvc.perform(post("/api/simulations").with(jwt().jwt(j -> j.claim("accountId", ACCOUNT)))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"amount":1000,"months":0}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_401_without_token() throws Exception {
        mvc.perform(post("/api/simulations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"amount":1000,"months":12}
                        """))
                .andExpect(status().isUnauthorized());
    }
}
