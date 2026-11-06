package dio.budgeting.application.output;

/** access: JWT curto para as chamadas da API. refresh: opaco, de uso único, usado só em /auth/refresh. */
public record SessionOutput(
        String accessToken,
        long expiresInSeconds,
        String refreshToken,
        long refreshExpiresInSeconds,
        String name,
        String email) {
}
