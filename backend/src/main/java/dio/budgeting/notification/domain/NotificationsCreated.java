package dio.budgeting.notification.domain;

import java.util.UUID;

/**
 * Evento publicado depois que há notificação nova para uma conta. Serve para avisar quem está conectado em tempo
 * real. Só carrega o id da conta, e o app busca o conteúdo pela API.
 */
public record NotificationsCreated(UUID accountId) {

    public static final String TOPIC = "notification.created";
    public static final String TYPE = "NotificationsCreated";
}
