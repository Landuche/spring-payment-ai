package dio.budgeting.transfer.domain;

import dio.budgeting.account.domain.AccountId;

import java.util.Optional;

public interface TransferRepository {
    Transfer save(Transfer transfer);

    /** Busca uma transferência já processada com a mesma chave, para tornar a operação idempotente. */
    Optional<Transfer> findByIdempotencyKey(AccountId origin, String idempotencyKey);
}
