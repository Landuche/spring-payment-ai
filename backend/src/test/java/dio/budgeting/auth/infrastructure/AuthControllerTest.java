package dio.budgeting.auth.infrastructure;

import dio.budgeting.account.application.InMemoryAccountRepository;
import dio.budgeting.account.domain.AccountRepository;
import dio.budgeting.audit.application.AuditLog;
import dio.budgeting.audit.application.InMemoryAuditEventRepository;
import dio.budgeting.audit.domain.AuditEventRepository;
import dio.budgeting.auth.application.AutenticarUseCase;
import dio.budgeting.auth.application.EncerrarSessaoUseCase;
import dio.budgeting.auth.application.InMemoryFailureCounterRepository;
import dio.budgeting.auth.application.InMemoryRefreshTokenRepository;
import dio.budgeting.auth.application.InMemoryUserRepository;
import dio.budgeting.auth.application.LoginProtectionService;
import dio.budgeting.auth.application.RegistrarUsuarioUseCase;
import dio.budgeting.auth.application.RenovarSessaoUseCase;
import dio.budgeting.auth.application.SessionIssuer;
import dio.budgeting.auth.domain.FailureCounterRepository;
import dio.budgeting.auth.domain.RefreshTokenRepository;
import dio.budgeting.auth.domain.UserRepository;
import dio.budgeting.shared.infrastructure.ClockConfig;
import dio.budgeting.shared.infrastructure.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        properties = "budgeting.security.jwt-secret=test-secret-com-pelo-menos-32-bytes-123456")
@AutoConfigureMockMvc
@Import({RegistrarUsuarioUseCase.class, AutenticarUseCase.class, JwtTokenProvider.class, LoginProtectionService.class,
        SessionIssuer.class, RenovarSessaoUseCase.class, EncerrarSessaoUseCase.class,
        AuditLog.class, ClockConfig.class, GlobalExceptionHandler.class, SecurityConfig.class,
        AuthControllerTest.TestRepositoryConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void should_register_then_login_returning_bearer_token() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Ana","email":"ana@example.com","password":"senha-forte-123"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value(not(emptyOrNullString())));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"ana@example.com","password":"senha-forte-123"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString())));
    }

    @Test
    void should_return_401_for_wrong_password() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Bia","email":"bia@example.com","password":"senha-forte-123"}
                        """))
                .andExpect(status().isCreated());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"bia@example.com","password":"errada-123"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_409_for_duplicate_email() throws Exception {
        var body = """
                {"name":"Caio","email":"caio@example.com","password":"senha-forte-123"}
                """;
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void should_return_400_for_invalid_registration() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"","email":"nao-e-email","password":"123"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void web_client_gets_refresh_token_only_in_httpOnly_cookie_and_refreshes_with_it() throws Exception {
        var result = mvc.perform(post("/auth/register").header("X-Client", "web")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Lia","email":"lia@example.com","password":"senha-forte-123"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andReturn();

        var cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();

        mvc.perform(post("/auth/refresh").header("X-Client", "web").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")));
    }

    @Test
    void refresh_without_any_token_returns_401() throws Exception {
        mvc.perform(post("/auth/refresh").header("X-Client", "web"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class TestRepositoryConfig {
        @Bean
        UserRepository userRepository() {
            return new InMemoryUserRepository();
        }

        @Bean
        AccountRepository accountRepository() {
            return new InMemoryAccountRepository();
        }

        @Bean
        FailureCounterRepository failureCounterRepository() {
            return new InMemoryFailureCounterRepository();
        }

        @Bean
        RefreshTokenRepository refreshTokenRepository() {
            return new InMemoryRefreshTokenRepository();
        }

        @Bean
        AuditEventRepository auditEventRepository() {
            return new InMemoryAuditEventRepository();
        }
    }
}
