package dio.budgeting.account.infrastructure;

import dio.budgeting.account.application.CurrentAccountProvider;
import dio.budgeting.account.domain.AccountId;
import dio.budgeting.auth.infrastructure.JwtTokenProvider;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Lê a conta do JWT da requisição corrente. Sem autenticação válida, nenhuma operação de conta é possível. */
@Component
public class SpringCurrentAccountProvider implements CurrentAccountProvider {

    @Override
    public AccountId current() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new InsufficientAuthenticationException("Autenticação necessária");
        }
        var claim = jwt.getToken().getClaimAsString(JwtTokenProvider.ACCOUNT_CLAIM);
        if (claim == null) {
            throw new InsufficientAuthenticationException("Token sem conta associada");
        }
        return new AccountId(UUID.fromString(claim));
    }
}
