package dio.budgeting;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regras de arquitetura verificadas sobre o código-fonte, sem dependência extra):
 * <ul>
 *   <li>{@code domain} não conhece frameworks nem camadas externas;</li>
 *   <li>{@code application} não conhece a infraestrutura nem o Spring AI: a IA é só um adaptador, e as ferramentas
 *       do modelo ficam em {@code assistant.infrastructure}.</li>
 * </ul>
 */
class LayerRulesTest {

    private static final Path MAIN = Path.of("src/main/java/dio/budgeting");
    private static final Pattern IMPORT = Pattern.compile("^import (?:static )?([\\w.]+)", Pattern.MULTILINE);

    @Test
    void domain_shouldNotDependOnFrameworksOrOuterLayers() throws IOException {
        assertThat(violations("domain", "org.springframework", "jakarta.", ".application.", ".infrastructure."))
                .as("imports proibidos na camada domain")
                .isEmpty();
    }

    @Test
    void application_shouldNotDependOnInfrastructureOrSpringAi() throws IOException {
        assertThat(violations("application", "org.springframework.ai", ".infrastructure."))
                .as("imports proibidos na camada application")
                .isEmpty();
    }

    /** Lista "arquivo -> import" cujo nome contém algum dos trechos proibidos, nos pacotes {@code <modulo>/<camada>}. */
    private static List<String> violations(String layer, String... forbidden) throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            return files
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> MAIN.relativize(p).getNameCount() > 2 && MAIN.relativize(p).getName(1).toString().equals(layer))
                    .flatMap(p -> importsOf(p).stream()
                            .filter(imp -> Arrays.stream(forbidden).anyMatch(imp::contains))
                            .map(imp -> MAIN.relativize(p) + " -> " + imp))
                    .toList();
        }
    }

    private static List<String> importsOf(Path file) {
        try {
            var source = Files.readString(file);
            return IMPORT.matcher(source).results().map(m -> m.group(1)).toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
