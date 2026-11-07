package dio.budgeting.notification.infrastructure;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class SocketMaintenance {
    private final SocketRegistry registry;

    SocketMaintenance(SocketRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelayString = "${budgeting.websocket.maintenance-interval-ms:30000}")
    void maintain() {
        registry.closeStaleConnections();
        registry.pingAll();
    }
}
