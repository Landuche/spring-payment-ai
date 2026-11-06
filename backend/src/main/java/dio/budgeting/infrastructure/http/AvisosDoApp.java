package dio.budgeting.infrastructure.http;

import dio.budgeting.infrastructure.http.request.AssistantEventRequest;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Transforma o aviso estruturado do app em uma mensagem que o Claudinho entende. Começa sempre com "[APP]", marca
 * que não é fala do cliente. O texto livre (nome, motivo) é higienizado: sem quebras de linha, sem tamanho
 * excessivo e sem "[APP]", para não conseguir se passar por um aviso do sistema nem injetar instruções.
 */
final class AvisosDoApp {
    private static final String PREFIXO = "[APP]";
    private static final int MAX_NOME = 60;
    private static final int MAX_MOTIVO = 200;
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private AvisosDoApp() {
    }

    static String mensagem(AssistantEventRequest evento) {
        var valor = formatar(evento.valor());
        var nome = limpar(evento.nomeDestinatario(), MAX_NOME);

        return switch (evento.tipo()) {
            case TRANSFERENCIA_CONFIRMADA -> "%s O cliente confirmou no cartão: a transferência de %s para %s foi ENVIADA com sucesso."
                    .formatted(PREFIXO, valor, nome);
            case TRANSFERENCIA_CANCELADA -> "%s O cliente cancelou no cartão a transferência de %s para %s. Nada foi enviado."
                    .formatted(PREFIXO, valor, nome);
            case TRANSFERENCIA_FALHOU -> "%s A transferência de %s para %s NÃO foi enviada. Motivo informado pelo sistema: %s. O cartão continua na tela para o cliente tentar de novo."
                    .formatted(PREFIXO, valor, nome, motivoOuPadrao(evento.motivo()));
        };
    }

    private static String motivoOuPadrao(String motivo) {
        var limpo = limpar(motivo, MAX_MOTIVO);
        return limpo.isEmpty() ? "não especificado" : limpo;
    }

    private static String formatar(BigDecimal valor) {
        return String.format(PT_BR, "R$ %,.2f", valor);
    }

    private static String limpar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        var limpo = texto
                .replaceAll("(?i)\\[\\s*APP\\s*]", " ")
                .replaceAll("[\\p{Cntrl}\\s]+", " ")
                .strip();
        return limpo.length() > max ? limpo.substring(0, max).strip() : limpo;
    }
}
