package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Gradle wrapper and project-level properties are compatible
 * with the Spring Boot 4.0.1 plugin applied in the configuration module.
 * Spring Boot 4.x requires Gradle 8.x+ for the plugins DSL and version catalog
 * support used by {@code alias(libs.plugins.springboot)}. These tests guard
 * against:
 * <ul>
 *   <li>Gradle wrapper downgrade below the minimum version required by
 *       Spring Boot 4.x and the version catalog feature</li>
 *   <li>Presence of gradle.properties overrides that could interfere with
 *       the Spring Boot plugin (e.g., forcing a different JVM, disabling
 *       configuration cache, overriding mainClass)</li>
 *   <li>Wrapper using a non-standard distribution type that omits the
 *       Kotlin DSL libraries needed for build.gradle.kts compilation</li>
 * </ul>
 */
class GradlePropertiesBootPluginCompatibilityTest {

    private static Path projectRoot;
    private static String wrapperProps;

    @BeforeAll
    static void loadWrapperProperties() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
        Path wrapperFile = projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties");
        assertThat(Files.exists(wrapperFile))
                .as("gradle-wrapper.properties must exist for reproducible builds")
                .isTrue();
        wrapperProps = Files.readString(wrapperFile);
    }

    @Test
    void wrapperVersion_shouldBeGradle8OrHigher() {
        String url = wrapperProps.lines()
                .filter(line -> line.startsWith("distributionUrl"))
                .map(line -> line.split("=", 2)[1].trim().replace("\\:", ":"))
                .findFirst()
                .orElse("");
        assertThat(url)
                .as("Gradle wrapper distribution URL must reference Gradle 8.x+ — "
                        + "Spring Boot 4.x requires Gradle 8 for version catalog support "
                        + "and the plugins DSL used by alias(libs.plugins.springboot)")
                .containsPattern("gradle-(8|9|[1-9]\\d)\\.");
    }

    @Test
    void wrapperDistribution_shouldUseBinOrAll() {
        String url = wrapperProps.lines()
                .filter(line -> line.startsWith("distributionUrl"))
                .map(line -> line.split("=", 2)[1].trim())
                .findFirst()
                .orElse("");
        assertThat(url)
                .as("Wrapper must use 'bin' or 'all' distribution — "
                        + "'src' distributions do not include precompiled Kotlin DSL libraries")
                .containsPattern("-(bin|all)\\.zip");
    }

    @Test
    void gradleProperties_shouldNotOverrideMainClass() {
        Path gradleProps = projectRoot.resolve("gradle.properties");
        if (!Files.exists(gradleProps)) {
            return;
        }
        try {
            String content = Files.readString(gradleProps);
            assertThat(content)
                    .as("gradle.properties must NOT set mainClass — the Spring Boot plugin "
                            + "auto-detects it via @SpringBootApplication annotation scanning")
                    .doesNotContainPattern("(?i)mainClass");
        } catch (IOException ignored) {
        }
    }

    @Test
    void gradleProperties_shouldNotOverrideSpringBootVersion() {
        Path gradleProps = projectRoot.resolve("gradle.properties");
        if (!Files.exists(gradleProps)) {
            return;
        }
        try {
            String content = Files.readString(gradleProps);
            assertThat(content)
                    .as("gradle.properties must NOT set a Spring Boot version — "
                            + "version management belongs exclusively in libs.versions.toml")
                    .doesNotContainPattern("(?i)springBoot.*version")
                    .doesNotContainPattern("(?i)spring\\.boot");
        } catch (IOException ignored) {
        }
    }

    @Test
    void wrapperProperties_shouldValidateDistributionUrl() {
        assertThat(wrapperProps)
                .as("gradle-wrapper.properties must enable distribution URL validation "
                        + "to guard against supply-chain attacks on the Gradle distribution")
                .contains("validateDistributionUrl=true");
    }

    @Test
    void wrapperProperties_shouldPointToOfficialGradleServices() {
        String url = wrapperProps.lines()
                .filter(line -> line.startsWith("distributionUrl"))
                .map(line -> line.split("=", 2)[1].trim().replace("\\:", ":"))
                .findFirst()
                .orElse("");
        assertThat(url)
                .as("Wrapper must download Gradle from official services.gradle.org — "
                        + "custom mirror URLs could serve tampered distributions")
                .contains("services.gradle.org");
    }
}
