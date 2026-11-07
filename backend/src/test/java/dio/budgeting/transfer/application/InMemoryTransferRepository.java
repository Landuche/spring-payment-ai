package dio.budgeting.transfer.application;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.transfer.domain.Transfer;
import dio.budgeting.transfer.domain.TransferRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryTransferRepository implements TransferRepository {
    private final List<Transfer> store = new ArrayList<>();

    @Override
    public Transfer save(Transfer transfer) {
        store.add(transfer);
        return transfer;
    }

    @Override
    public Optional<Transfer> findByIdempotencyKey(AccountId origin, String idempotencyKey) {
        return store.stream()
                .filter(t -> t.origin().equals(origin) && t.idempotencyKey().equals(idempotencyKey))
                .findFirst();
    }

    public int count() {
        return store.size();
    }
}
