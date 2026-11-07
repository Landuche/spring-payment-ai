package dio.budgeting.outbox.domain;

import java.util.List;

public interface OutboxRepository {
    OutboxEvent save(OutboxEvent event);

    /**
     * Eventos ainda não publicados, do mais antigo ao mais novo, travados para este processo: duas instâncias da
     * aplicação não publicam o mesmo evento ao mesmo tempo.
     */
    List<OutboxEvent> findPendingForUpdate(int limit);
}
