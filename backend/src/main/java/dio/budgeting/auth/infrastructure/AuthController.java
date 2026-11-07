package dio.budgeting.auth.infrastructure;

import dio.budgeting.auth.application.LoginUseCase;
import dio.budgeting.auth.application.LogoutUseCase;
import dio.budgeting.auth.application.RegisterUserUseCase;
import dio.budgeting.auth.application.RefreshSessionUseCase;
import dio.budgeting.auth.application.SessionOutput;
import dio.budgeting.auth.domain.AuthenticationFailedException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Clientes nativos recebem o refresh token no corpo JSON (guardado no SecureStore).
 * O cliente web (header {@code X-Client: web}) recebe o refresh token apenas em cookie HttpOnly:
 * JavaScript não consegue lê-lo, então um XSS não consegue roubar a sessão de longa duração.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    static final String WEB_CLIENT_HEADER = "X-Client";
    static final String WEB_CLIENT = "web";
    static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/auth";

    private final RegisterUserUseCase registerUser;
    private final LoginUseCase login;
    private final RefreshSessionUseCase refreshSession;
    private final LogoutUseCase logout;

    public AuthController(RegisterUserUseCase registerUser, LoginUseCase login,
                          RefreshSessionUseCase refreshSession, LogoutUseCase logout) {
        this.registerUser = registerUser;
        this.login = login;
        this.refreshSession = refreshSession;
        this.logout = logout;
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    /** Opcional: o web envia o refresh token pelo cookie; o nativo envia pelo corpo. */
    public record RefreshRequest(@Size(max = 128) String refreshToken) {
    }

    public record SessionResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            String refreshToken,
            long refreshExpiresIn,
            String name,
            String email) {

        static SessionResponse from(SessionOutput output, boolean webClient) {
            return new SessionResponse(output.accessToken(), "Bearer", output.expiresInSeconds(),
                    webClient ? null : output.refreshToken(), output.refreshExpiresInSeconds(),
                    output.name(), output.email());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<SessionResponse> register(@Valid @RequestBody RegisterRequest request,
                                                    HttpServletRequest httpRequest) {
        var session = registerUser.execute(request.email(), request.name(), request.password());
        return sessionResponse(HttpStatus.CREATED, session, httpRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request,
                                                 HttpServletRequest httpRequest) {
        var session = login.execute(request.email(), request.password());
        return sessionResponse(HttpStatus.OK, session, httpRequest);
    }

    /** Rotação: o refresh token usado deixa de valer e um novo par é devolvido. */
    @PostMapping("/refresh")
    public ResponseEntity<SessionResponse> refresh(@RequestBody(required = false) RefreshRequest request,
                                                   HttpServletRequest httpRequest) {
        var token = refreshTokenFrom(request, httpRequest)
                .orElseThrow(AuthenticationFailedException::new);
        return sessionResponse(HttpStatus.OK, refreshSession.execute(token), httpRequest);
    }

    /** Revoga a sessão inteira. Sempre 204, mesmo para token desconhecido ou ausente. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request,
                                       HttpServletRequest httpRequest) {
        refreshTokenFrom(request, httpRequest).ifPresent(logout::execute);
        var clear = refreshCookie("", 0);
        return isWeb(httpRequest)
                ? ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clear.toString()).build()
                : ResponseEntity.noContent().build();
    }

    private ResponseEntity<SessionResponse> sessionResponse(HttpStatus status, SessionOutput session,
                                                            HttpServletRequest httpRequest) {
        var builder = ResponseEntity.status(status);
        if (isWeb(httpRequest)) {
            builder.header(HttpHeaders.SET_COOKIE,
                    refreshCookie(session.refreshToken(), session.refreshExpiresInSeconds()).toString());
        }
        return builder.body(SessionResponse.from(session, isWeb(httpRequest)));
    }

    private static Optional<String> refreshTokenFrom(RefreshRequest request, HttpServletRequest httpRequest) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return Optional.of(request.refreshToken());
        }
        var cookies = httpRequest.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private static boolean isWeb(HttpServletRequest httpRequest) {
        return WEB_CLIENT.equalsIgnoreCase(httpRequest.getHeader(WEB_CLIENT_HEADER));
    }

    /** Secure + SameSite=Strict: o cookie só segue para o próprio /auth, nunca em requisições cross-site. */
    private static ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
