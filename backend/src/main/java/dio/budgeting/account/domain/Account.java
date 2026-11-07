package dio.budgeting.account.domain;

import dio.budgeting.auth.domain.UserId;

import java.security.SecureRandom;
import java.util.Objects;

/** Conta corrente de um usuário. O saldo é derivado do extrato, não armazenado. */
public record Account(AccountId id, UserId ownerId, String number) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public Account {
        Objects.requireNonNull(id, "O identificador da conta é obrigatório");
        Objects.requireNonNull(ownerId, "O titular da conta é obrigatório");
        if (number == null || !number.matches("\\d{10}")) {
            throw new IllegalArgumentException("Número de conta inválido");
        }
    }

    public static Account open(UserId ownerId) {
        return new Account(new AccountId(), ownerId, generateNumber());
    }

    /** Número de 10 dígitos gerado com fonte criptograficamente segura. Unicidade é garantida pelo banco. */
    static String generateNumber() {
        return String.format("%010d", RANDOM.nextLong(10_000_000_000L));
    }
}
