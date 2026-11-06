package dio.budgeting.auth.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.auth.domain.UserId;


public interface TokenProvider {
    IssuedToken issue(UserId userId, AccountId accountId);

    record IssuedToken(String value, long expiresInSeconds) {
    }
}
