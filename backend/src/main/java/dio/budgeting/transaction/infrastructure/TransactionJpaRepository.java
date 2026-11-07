package dio.budgeting.transaction.infrastructure;

import dio.budgeting.account.domain.AccountId;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionRepository;

import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
class TransactionJpaRepository implements TransactionRepository {
    private final TransactionEntityRepository entityRepository;

    TransactionJpaRepository(TransactionEntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        return entityRepository.save(TransactionEntity.from(transaction)).toDomain();
    }

    @Override
    public List<Transaction> findAll(AccountId accountId) {
        return toDomain(entityRepository.findAllByAccountId(accountId.uuid()));
    }

    @Override
    public List<Transaction> findAllBetween(AccountId accountId, LocalDate start, LocalDate end) {
        return toDomain(entityRepository.findAllByAccountIdAndOccurredAtBetween(accountId.uuid(), start, end));
    }

    @Override
    public List<Transaction> findAllBetween(AccountId accountId, LocalDate start, LocalDate end, Category category) {
        return toDomain(entityRepository.findAllByAccountIdAndOccurredAtBetweenAndCategory(
                accountId.uuid(), start, end, category));
    }

    private static List<Transaction> toDomain(List<TransactionEntity> entities) {
        return entities.stream().map(TransactionEntity::toDomain).toList();
    }
}
