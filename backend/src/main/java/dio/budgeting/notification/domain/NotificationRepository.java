package dio.budgeting.notification.domain;

import dio.budgeting.account.domain.AccountId;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository {

    /**
     * Grava o aviso se ainda não existir um para o mesmo evento e a mesma conta. É isso que torna o consumidor
     * idempotente: o Kafka pode entregar o mesmo evento mais de uma vez.
     *
     * @return true se gravou, false se já existia
     */
    boolean saveIfAbsent(Notification notification);

    /** Os mais recentes primeiro. */
    List<Notification> findLatest(AccountId accountId, int limit);

    long countUnread(AccountId accountId);

    /** @return quantos avisos foram marcados como lidos */
    int markAllAsRead(AccountId accountId, Instant now);
}
