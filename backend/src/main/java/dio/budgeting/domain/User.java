package dio.budgeting.domain;

import java.util.Locale;
import java.util.Objects;

/** Usuário autenticável. A senha chega já codificada (hash); o domínio nunca vê a senha em texto. */
public record User(UserId id, String email, String name, String passwordHash) {
    public User {
        Objects.requireNonNull(id, "O identificador é obrigatório");
        Objects.requireNonNull(passwordHash, "O hash da senha é obrigatório");
        if (email == null || email.isBlank()) {
            throw new BusinessRuleViolationException("O e-mail é obrigatório");
        }
        if (name == null || name.isBlank()) {
            throw new BusinessRuleViolationException("O nome é obrigatório");
        }
        email = email.strip().toLowerCase(Locale.ROOT);
        name = name.strip();
    }
}
