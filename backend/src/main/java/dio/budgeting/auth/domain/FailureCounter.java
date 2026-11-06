package dio.budgeting.auth.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Contador de falhas de autenticação por chave (hoje "email:...").
 * Janela fixa: as falhas só acumulam dentro de {@code windowStart + janela}. Ao atingir o máximo, bloqueia por um período.
 * Imutável: cada operação devolve o novo estado, que o chamador persiste.
 */
public record FailureCounter(String key, int failures, Instant windowStart, Instant blockedUntil) {

    public static FailureCounter fresh(String key) {
        return new FailureCounter(key, 0, null, null);
    }

    public boolean isBlocked(Instant now) {
        return blockedUntil != null && now.isBefore(blockedUntil);
    }

    /** Registra uma falha. Ao atingir {@code max} dentro da janela, devolve o estado bloqueado até {@code now + block}. */
    public FailureCounter registrarFalha(Instant now, int max, Duration janela, Duration block) {
        if (isBlocked(now)) {
            return this;
        }
        boolean novaJanela = windowStart == null || !now.isBefore(windowStart.plus(janela));
        int base = novaJanela ? 0 : failures;
        Instant inicio = novaJanela ? now : windowStart;
        int next = base + 1;

        if (next >= max) {
            return new FailureCounter(key, 0, null, now.plus(block));
        }
        return new FailureCounter(key, next, inicio, null);
    }

    /** Zera o contador depois de um login bem-sucedido. */
    public FailureCounter limpar() {
        return fresh(key);
    }
}
