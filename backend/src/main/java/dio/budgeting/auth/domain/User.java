package dio.budgeting.auth.domain;

import dio.budgeting.shared.domain.BusinessRuleViolationException;

import java.util.Locale;
import java.util.Objects;

/** Usuário autenticável. A senha chega já codificada. */
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

    /** Nome para terceiros: só o primeiro nome por extenso. "Ana Paula Souza" vira "Ana P. S.". */
    public String maskedName() {
        var parts = name.split("\\s+");
        var masked = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            masked.append(' ').append(parts[i].charAt(0)).append('.');
        }
        return masked.toString();
    }
}
