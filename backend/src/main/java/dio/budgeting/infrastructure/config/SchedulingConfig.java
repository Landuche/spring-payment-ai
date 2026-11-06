package dio.budgeting.infrastructure.config;

import dio.budgeting.application.LimpezaRegistrosUseCase;
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

    private final LimpezaRegistrosUseCase limpeza;

    public SchedulingConfig(LimpezaRegistrosUseCase limpeza) {
        this.limpeza = limpeza;
    }

    @Scheduled(cron = "${budgeting.cleanup.cron:0 15 3 * * *}")
    void limparRegistrosExpirados() {
        try {
            limpeza.executar();
        } catch (RuntimeException e) {
            // Falha na limpeza não pode derrubar a aplicação: o próximo ciclo tenta de novo.
            log.error("Falha na limpeza de registros expirados", e);
        }
    }
}
