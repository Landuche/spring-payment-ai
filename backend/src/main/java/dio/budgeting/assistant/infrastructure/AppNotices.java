package dio.budgeting.assistant.infrastructure;


import java.math.BigDecimal;
import java.util.Locale;

/**
 * Transforma o aviso estruturado do app em uma mensagem que o Claudinho entende. Começa sempre com "[APP]", marca
 * que não é fala do cliente. O texto livre (nome, motivo) é higienizado: sem quebras de linha, sem tamanho
 * excessivo e sem "[APP]", para não conseguir se passar por um aviso do sistema nem injetar instruções.
 */
final class AppNotices {
    private static final String PREFIX = "[APP]";
    private static final int MAX_NAME = 60;
    private static final int MAX_REASON = 200;
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private AppNotices() {
    }

    static String message(AssistantEventRequest event) {
        var amount = format(event.amount());
        var name = sanitize(event.recipientName(), MAX_NAME);

        return switch (event.type()) {
            case TRANSFER_CONFIRMED -> "%s O cliente confirmou no cartão: a transferência de %s para %s foi ENVIADA com sucesso."
                    .formatted(PREFIX, amount, name);
            case TRANSFER_CANCELED -> "%s O cliente cancelou no cartão a transferência de %s para %s. Nada foi enviado."
                    .formatted(PREFIX, amount, name);
            case TRANSFER_FAILED -> "%s A transferência de %s para %s NÃO foi enviada. Motivo informado pelo sistema: %s. O cartão continua na tela para o cliente tentar de novo."
                    .formatted(PREFIX, amount, name, reasonOrDefault(event.reason()));
        };
    }

    private static String reasonOrDefault(String reason) {
        var cleaned = sanitize(reason, MAX_REASON);
        return cleaned.isEmpty() ? "não especificado" : cleaned;
    }

    private static String format(BigDecimal amount) {
        return String.format(PT_BR, "R$ %,.2f", amount);
    }

    private static String sanitize(String text, int max) {
        if (text == null) {
            return "";
        }
        var cleaned = text
                .replaceAll("(?i)\\[\\s*APP\\s*]", " ")
                .replaceAll("[\\p{Cntrl}\\s]+", " ")
                .strip();
        return cleaned.length() > max ? cleaned.substring(0, max).strip() : cleaned;
    }
}
