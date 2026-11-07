package dio.budgeting.auth.application;

public record SessionOutput(
        String accessToken,
        long expiresInSeconds,
        String refreshToken,
        long refreshExpiresInSeconds,
        String name,
        String email) {
}
