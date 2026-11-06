package dio.budgeting.application.port;

import dio.budgeting.domain.AccountId;
import dio.budgeting.domain.UserId;

public interface TokenProvider {
    IssuedToken issue(UserId userId, AccountId accountId);

    record IssuedToken(String value, long expiresInSeconds) {
    }
}
