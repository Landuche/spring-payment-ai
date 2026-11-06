package dio.budgeting.infrastructure.http;

import dio.budgeting.application.AuditLog;
import dio.budgeting.application.AutenticarUseCase;
import dio.budgeting.application.InMemoryAccountRepository;
import dio.budgeting.application.InMemoryAuditEventRepository;
import dio.budgeting.domain.AuditEventRepository;
import dio.budgeting.application.EncerrarSessaoUseCase;
import dio.budgeting.application.InMemoryRefreshTokenRepository;
import dio.budgeting.application.RenovarSessaoUseCase;
import dio.budgeting.application.SessionIssuer;
import dio.budgeting.domain.RefreshTokenRepository;
import dio.budgeting.application.InMemoryFailureCounterRepository;
import dio.budgeting.application.InMemoryUserRepository;
import dio.budgeting.application.LoginProtectionService;
import dio.budgeting.application.RegistrarUsuarioUseCase;
import dio.budgeting.domain.AccountRepository;
import dio.budgeting.domain.FailureCounterRepository;
import dio.budgeting.domain.UserRepository;
import dio.budgeting.infrastructure.config.ClockConfig;
import dio.budgeting.infrastructure.http.exception.GlobalExceptionHandler;
import dio.budgeting.infrastructure.security.JwtTokenProvider;
import dio.budgeting.infrastructure.security.SecurityConfig;
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
