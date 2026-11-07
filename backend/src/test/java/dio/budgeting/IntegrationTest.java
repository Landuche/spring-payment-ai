package dio.budgeting;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sobe a aplicação inteira contra Postgres e Kafka reais. Os testes com esta anotação compartilham o mesmo contexto
 * (mesmas propriedades e mesmos containers), então ele é montado uma vez só.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "budgeting.security.jwt-secret=segredo-apenas-para-testes-com-mais-de-32-bytes!!",
        "spring.ai.openai.api-key=chave-de-teste",
        "budgeting.outbox.poll-interval-ms=200"
})
@Import(ContainersConfig.class)
public @interface IntegrationTest {
}
