package dio.budgeting.cleanup.infrastructure;

import dio.budgeting.cleanup.application.CleanupRecordsUseCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Agenda a limpeza de registros expirados. Padrão: 03:15 todo dia (horário do Clock da aplicação).
 * Para mudar o horário, defina {@code budgeting.cleanup.cron}.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);

    private final CleanupRecordsUseCase cleanup;

    public SchedulingConfig(CleanupRecordsUseCase cleanup) {
        this.cleanup = cleanup;
    }

    @Scheduled(cron = "${budgeting.cleanup.cron:0 15 3 * * *}")
    void cleanUpExpiredRecords() {
        try {
            cleanup.execute();
        } catch (RuntimeException e) {
            // Falha na limpeza não pode derrubar a aplicação: o próximo ciclo tenta de novo.
            log.error("Falha na limpeza de registros expirados", e);
        }
    }
}
