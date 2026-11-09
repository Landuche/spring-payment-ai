package dio.budgeting.transfer.application;

import dio.budgeting.account.application.CurrentAccountProvider;
import dio.budgeting.account.application.LookupGuardService;
import dio.budgeting.account.domain.Account;
import dio.budgeting.account.domain.AccountId;
import dio.budgeting.account.domain.AccountRepository;
import dio.budgeting.audit.application.AuditLog;
import dio.budgeting.audit.domain.AuditEventType;
import dio.budgeting.auth.domain.User;
import dio.budgeting.auth.domain.UserRepository;
import dio.budgeting.outbox.application.OutboxRecorder;
import dio.budgeting.shared.domain.BusinessRuleViolationException;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.shared.domain.NotFoundException;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionRepository;
import dio.budgeting.transaction.domain.TransactionType;
import dio.budgeting.transfer.domain.Transfer;
import dio.budgeting.transfer.domain.TransferCompleted;
import dio.budgeting.transfer.domain.TransferRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Transferência entre contas do sistema, por número de conta ou e-mail do titular.
 * <ul>
 *   <li>Atômica: débito, crédito e registro da transferência compartilham uma transação de banco.</li>
 *   <li>Serializada: as duas contas são travadas em ordem fixa, evitando deadlock.</li>
 *   <li>Idempotente: repetir a mesma Idempotency-Key devolve a transferência já processada.</li>
 * </ul>
 */
@Service
public class TransferUseCase {
    private final TransferRepository transferRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CurrentAccountProvider currentAccount;
    private final LookupGuardService lookupGuard;
    private final AuditLog audit;
    private final OutboxRecorder outbox;
    private final Clock clock;

    public TransferUseCase(TransferRepository transferRepository, TransactionRepository transactionRepository,
                           AccountRepository accountRepository, UserRepository userRepository,
                           CurrentAccountProvider currentAccount, LookupGuardService lookupGuard,
                           AuditLog audit, OutboxRecorder outbox, Clock clock) {
        this.transferRepository = transferRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.currentAccount = currentAccount;
        this.lookupGuard = lookupGuard;
        this.audit = audit;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public TransferOutput execute(String destinationNumber, String destinationEmail, BigDecimal amount,
                                  String description, String idempotencyKey) {
        var origin = currentAccount.current();

        var existing = transferRepository.findByIdempotencyKey(origin, idempotencyKey);
        if (existing.isPresent()) {
            return output(existing.get(), true);
        }

        // Toda tentativa de localizar destinatário conta, inclusive as que falham: senão o endpoint de transferência
        // viraria uma forma de enumerar contas, contornando o limite da busca.
        lookupGuard.recordLookup(origin);

        var money = Money.ofPositiveReais(amount);
        var destination = resolveDestination(destinationNumber, destinationEmail);

        if (destination.id().equals(origin)) {
            throw new BusinessRuleViolationException("Não é possível transferir para a própria conta");
        }

        lockInOrder(origin, destination.id());

        var replayed = transferRepository.findByIdempotencyKey(origin, idempotencyKey);
        if (replayed.isPresent()) {
            return output(replayed.get(), true);
        }

        var sourceAccount = accountRepository.findById(origin)
                .orElseThrow(() -> new IllegalStateException("Conta de origem inexistente"));
        var balance = balance(origin);
        if (money.cents() > balance.cents()) {
            // Recusa fica registrada mesmo com o rollback.
            audit.failure(AuditEventType.TRANSFER_REFUSED, sourceAccount.ownerId(), origin, null,
                    destination.id().uuid().toString(), "INSUFFICIENT_BALANCE");
            throw new BusinessRuleViolationException(
                    "Saldo insuficiente: saldo atual de " + balance.toReais() + " reais não cobre a transferência de "
                            + money.toReais() + " reais");
        }

        var now = Instant.now(clock);
        var transfer = transferRepository.save(new Transfer(
                UUID.randomUUID(), origin, destination.id(), money, description, idempotencyKey, now));

        var today = LocalDate.ofInstant(now, clock.getZone());
        var destinationHolder = holder(destination);
        var sourceHolder = holder(sourceAccount);

        // Com descrição informada, ela aparece no extrato dos dois lados.
        var providedDescription = transfer.description();
        var sentText = providedDescription != null ? providedDescription : "Transferência para " + destinationHolder.name();
        var receivedText = providedDescription != null ? providedDescription : "Transferência de " + sourceHolder.name();

        transactionRepository.save(Transaction.fromTransfer(transfer.id(), origin, sentText, money,
                TransactionType.TRANSFER_SENT, today, now));
        transactionRepository.save(Transaction.fromTransfer(transfer.id(), destination.id(), receivedText, money,
                TransactionType.TRANSFER_RECEIVED, today, now));

        audit.success(AuditEventType.TRANSFER_COMPLETED, sourceAccount.ownerId(), origin, transfer.id().toString());

        // Mesma transação da transferência: o evento só existe se a transferência existir (padrão outbox).
        outbox.record(TransferCompleted.TOPIC, transfer.id(), TransferCompleted.TYPE, new TransferCompleted(
                transfer.id(), origin.uuid(), destination.id().uuid(), money.cents(),
                sourceHolder.maskedName(), destinationHolder.maskedName(), now));

        // Uma transferência concluída zera o contador de buscas da conta.
        lookupGuard.reset(origin);

        return output(transfer, false);
    }

    private Account resolveDestination(String number, String email) {
        var hasNumber = number != null && !number.isBlank();
        var hasEmail = email != null && !email.isBlank();
        if (hasNumber == hasEmail) {
            throw new BusinessRuleViolationException("Informe o número da conta ou o e-mail do destinatário (apenas um)");
        }

        if (hasNumber) {
            var digits = number.replaceAll("\\D", "");
            if (!digits.matches("\\d{10}")) {
                throw new BusinessRuleViolationException("Número de conta de destino inválido");
            }
            return accountRepository.findByNumber(digits)
                    .orElseThrow(() -> new NotFoundException("Conta de destino não encontrada"));
        }

        return userRepository.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .flatMap(user -> accountRepository.findByOwner(user.id()))
                .orElseThrow(() -> new NotFoundException("Conta de destino não encontrada"));
    }

    /** Trava as duas contas sempre na mesma ordem, para duas transferências cruzadas nunca se bloquearem. */
    private void lockInOrder(AccountId a, AccountId b) {
        Stream.of(a, b)
                .sorted(Comparator.comparing(id -> id.uuid().toString()))
                .forEach(accountRepository::lockForUpdate);
    }

    private Money balance(AccountId accountId) {
        return transactionRepository.findAll(accountId).stream()
                .map(Transaction::signedAmount)
                .reduce(Money.ZERO, Money::plus);
    }

    private User holder(Account account) {
        return userRepository.findById(account.ownerId())
                .orElseThrow(() -> new IllegalStateException("Conta sem titular: " + account.id()));
    }

    private static TransferOutput output(Transfer transfer, boolean replayed) {
        var description = transfer.description() != null ? transfer.description() : "Transferência";
        return new TransferOutput(transfer.id().toString(), transfer.amount().toReais(), description,
                transfer.createdAt(), replayed);
    }
}
