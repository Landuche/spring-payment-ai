package dio.budgeting.auth.application;

import dio.budgeting.account.domain.Account;
import dio.budgeting.auth.domain.RefreshToken;
import dio.budgeting.auth.domain.RefreshTokenRepository;
import dio.budgeting.auth.domain.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Único ponto que emite sessões (access + refresh), usado no cadastro, no login e na renovação. */
@Service
public class SessionIssuer {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;
    private final Duration refreshTtl;

    public SessionIssuer(TokenProvider tokenProvider, RefreshTokenRepository refreshTokens, Clock clock,
                         @Value("${budgeting.security.refresh-ttl:P30D}") Duration refreshTtl) {
        this.tokenProvider = tokenProvider;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
        this.refreshTtl = refreshTtl;
    }

    /** Nova sessão: começa uma família nova de refresh tokens. */
    public SessionOutput emitir(User user, Account account) {
        return emitir(user, account, UUID.randomUUID());
    }

    /** Renovação: o novo refresh token continua a família do anterior. */
    public SessionOutput emitir(User user, Account account, UUID familyId) {
        var now = Instant.now(clock);
        var access = tokenProvider.issue(user.id(), account.id());

        var raw = gerarToken();
        refreshTokens.save(new RefreshToken(RefreshToken.hash(raw), user.id(), familyId,
                now.plus(refreshTtl), null, null));

        return new SessionOutput(access.value(), access.expiresInSeconds(), raw, refreshTtl.toSeconds(),
                user.name(), user.email());
    }

    /** 256 bits de entropia, codificados em base64url sem padding. */
    static String gerarToken() {
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
