package dio.budgeting.infrastructure.security;

import dio.budgeting.application.port.TokenProvider;
import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.UserId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class JwtTokenProvider implements TokenProvider {
    static final String ACCOUNT_CLAIM = "accountId";

    private final JwtEncoder encoder;
    private final Duration ttl;

    JwtTokenProvider(JwtEncoder encoder, @Value("${budgeting.security.jwt-ttl:PT15M}") Duration ttl) {
        this.encoder = encoder;
        this.ttl = ttl;
    }

    @Override
    public IssuedToken issue(UserId userId, AccountId accountId) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(userId.uuid().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim(ACCOUNT_CLAIM, accountId.uuid().toString())
                .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, ttl.toSeconds());
    }
}
