package dio.budgeting;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Sobe a aplicação inteira contra um Postgres descartável (Testcontainers), rodando as migrações Flyway de verdade.
 * Só exige o Docker em execução: não depende do banco do compose, do arquivo .env nem de variáveis de ambiente.
 */
@Testcontainers
@SpringBootTest(properties = {
        "budgeting.security.jwt-secret=segredo-apenas-para-testes-com-mais-de-32-bytes!!",
        "spring.ai.openai.api-key=chave-de-teste"
})
class BudgetingApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void contextLoads() {
    }

}
