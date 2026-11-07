package dio.budgeting.transfer.domain;

import dio.budgeting.account.domain.AccountId;

import java.util.Optional;

public interface TransferRepository {
    Transfer save(Transfer transfer);

    Optional<Transfer> findByIdempotencyKey(AccountId origin, String idempotencyKey);
}
