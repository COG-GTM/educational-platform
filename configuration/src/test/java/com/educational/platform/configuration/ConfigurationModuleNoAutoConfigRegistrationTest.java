package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the configuration module does not register any custom
 * auto-configuration classes via META-INF service files. The Spring Boot
 * plugin enables auto-configuration discovery; if the configuration module
 * accidentally ships a {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * or legacy {@code META-INF/spring.factories} file, it could interfere with
 * the standard auto-configuration ordering or introduce duplicate bean
 * definitions.
 * <p>
 * The configuration module is the composition root — it consumes
 * auto-configuration from library modules but must not declare its own.
 * Complements {@link ConfigurationModuleOnlyBootEntryPointTest} which guards
 * against multiple @SpringBootApplication classes, and
 * {@link ApplicationAnnotationCompletenessTest} which guards the annotation set.
 */
class ConfigurationModuleNoAutoConfigRegistrationTest {

    private static Path projectRoot;

    @BeforeAll
    static void resolveProjectRoot() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
    }

    @Test
    void mainResources_shouldNotContainSpringFactories() {
        Path springFactories = projectRoot.resolve(
                "configuration/src/main/resources/META-INF/spring.factories");
        assertThat(Files.exists(springFactories))
                .as("META-INF/spring.factories must NOT exist in the configuration module — "
                        + "the composition root should not register auto-configurations; "
                        + "this legacy file would interfere with Spring Boot plugin's "
                        + "auto-configuration ordering")
                .isFalse();
    }

    @Test
    void mainResources_shouldNotContainAutoConfigurationImports() {
        Path autoConfigImports = projectRoot.resolve(
                "configuration/src/main/resources/META-INF/spring/"
                        + "org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        assertThat(Files.exists(autoConfigImports))
                .as("AutoConfiguration.imports must NOT exist in the configuration module — "
                        + "the composition root should not register auto-configurations; "
                        + "auto-configuration belongs in library modules")
                .isFalse();
    }

    @Test
    void mainResources_shouldNotContainMetaInfSpringDirectory() {
        Path metaInfSpring = projectRoot.resolve(
                "configuration/src/main/resources/META-INF/spring");
        assertThat(Files.exists(metaInfSpring))
                .as("META-INF/spring/ directory must NOT exist in the configuration module — "
                        + "the composition root does not provide auto-configuration")
                .isFalse();
    }

    @Test
    void mainResources_shouldNotContainMetaInfDirectory() {
        Path metaInf = projectRoot.resolve(
                "configuration/src/main/resources/META-INF");
        assertThat(Files.exists(metaInf))
                .as("META-INF/ directory must NOT exist in the configuration module's "
                        + "main resources — manifest attributes and service files are "
                        + "managed by the Spring Boot plugin's bootJar task")
                .isFalse();
    }

    @Test
    void testResources_shouldNotContainAutoConfigurationImports() throws IOException {
        Path testResources = projectRoot.resolve("configuration/src/test/resources");
        if (Files.exists(testResources)) {
            boolean hasAutoConfigFile = Files.walk(testResources)
                    .anyMatch(p -> p.toString().contains("AutoConfiguration.imports")
                            || p.getFileName().toString().equals("spring.factories"));
            assertThat(hasAutoConfigFile)
                    .as("Test resources must NOT contain auto-configuration registration files")
                    .isFalse();
        }
    }
}
