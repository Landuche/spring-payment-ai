package dio.budgeting.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Valor monetário em centavos. Nunca usa double para dinheiro.
 * Aceita valores negativos (ex.: saldo), a positividade é regra de cada caso de uso.
 */
public record Money(long cents) {

    public static final Money ZERO = new Money(0);

    public static Money ofReais(BigDecimal reais) {
        Objects.requireNonNull(reais, "O valor é obrigatório");
        try {
            return new Money(reais.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact());
        } catch (ArithmeticException e) {
            throw new BusinessRuleViolationException("Valor monetário inválido: " + reais);
        }
    }

    public static Money ofPositiveReais(BigDecimal reais) {
        var money = ofReais(reais);
        if (money.cents() <= 0) {
            throw new BusinessRuleViolationException("O valor deve ser maior que zero");
        }
        return money;
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(cents, other.cents));
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(cents, other.cents));
    }

    public BigDecimal toReais() {
        return BigDecimal.valueOf(cents, 2);
    }
}
