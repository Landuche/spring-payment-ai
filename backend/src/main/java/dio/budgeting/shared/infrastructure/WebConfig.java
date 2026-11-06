package dio.budgeting.shared.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS para o cliente web do Expo em desenvolvimento.
 * Liberado apenas para localhost; em produção, a origem deve ser configurada explicitamente.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                // Necessário para o navegador enviar e aceitar o cookie do refresh token (cliente web).
                .allowCredentials(true)
                // Sem isso, o navegador esconde Retry-After do JavaScript e a tela não consegue mostrar o tempo de espera.
                .exposedHeaders("Retry-After");
    }
}
