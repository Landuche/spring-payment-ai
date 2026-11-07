package dio.budgeting;

import dio.budgeting.account.application.InMemoryAccountRepository;
import dio.budgeting.account.domain.Account;
import dio.budgeting.account.domain.AccountId;
import dio.budgeting.auth.application.InMemoryUserRepository;
import dio.budgeting.auth.domain.User;
import dio.budgeting.auth.domain.UserId;
import dio.budgeting.shared.domain.Money;
import dio.budgeting.transaction.application.InMemoryTransactionRepository;
import dio.budgeting.transaction.domain.Category;
import dio.budgeting.transaction.domain.Transaction;
import dio.budgeting.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Contas e saldo inicial em memória, para os testes de caso de uso que precisam de dois titulares. */
public final class TestData {

    private TestData() {
    }

    /** Cria o usuário (e-mail derivado do nome) e a conta com o número informado. */
    public static Account newAccount(InMemoryUserRepository users, InMemoryAccountRepository accounts,
                                     String name, String number) {
        var user = users.save(new User(new UserId(), name.toLowerCase().replace(' ', '.') + "@example.com", name, "hash"));
        return accounts.save(new Account(new AccountId(), user.id(), number));
    }

    /** Lança uma receita de saldo inicial na conta. */
    public static void deposit(InMemoryTransactionRepository transactions, Account account, String amount) {
        transactions.save(Transaction.create(account.id(), "Saldo inicial",
                new Money(new BigDecimal(amount).movePointRight(2).longValueExact()),
                Category.AUTO, TransactionType.INCOME, LocalDate.of(2026, 11, 1)));
    }
}
