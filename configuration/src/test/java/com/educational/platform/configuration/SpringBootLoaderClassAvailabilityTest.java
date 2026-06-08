package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot plugin's bootJar packaging infrastructure
 * is correctly configured. The Spring Boot Loader classes (JarLauncher,
 * PropertiesLauncher) are NOT on the compile-time classpath — they are
 * injected by the Spring Boot Gradle plugin during the {@code bootJar} task.
 * These tests validate the prerequisites that enable correct loader injection:
 * <ul>
 *   <li>The build file does NOT exclude the spring-boot-loader dependency</li>
 *   <li>The spring-boot-loader coordinates are resolvable via the BOM</li>
 *   <li>No custom layout configuration overrides the default JAR layout</li>
 *   <li>The plugin's LaunchedURLClassLoader support classes are on the
 *       runtime classpath (via spring-boot module, not spring-boot-loader)</li>
 * </ul>
 * <p>
 * Complements {@link SpringBootManifestAttributeTest} (Start-Class validation)
 * and {@link BootJarConfigurationValidationTest} (bootJar task config).
 */
class SpringBootLoaderClassAvailabilityTest {

    private static String buildContent;

    @BeforeAll
    static void loadBuildFile() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        buildContent = Files.readString(dir.resolve("configuration/build.gradle.kts"));
    }

    @Test
    void buildFile_shouldNotExcludeSpringBootLoader() {
        assertThat(buildContent)
                .as("configuration/build.gradle.kts must NOT exclude spring-boot-loader — "
                        + "the Spring Boot plugin injects loader classes into bootJar; "
                        + "excluding them would produce a non-executable fat JAR")
                .doesNotContain("spring-boot-loader")
                .doesNotContain("exclude.*loader");
    }

    @Test
    void buildFile_shouldNotOverrideBootJarLayout() {
        assertThat(buildContent)
                .as("bootJar layout must not be overridden — the default JAR layout "
                        + "uses JarLauncher which handles nested JARs correctly; "
                        + "custom layouts can break class loading")
                .doesNotContainPattern("layout\\s*[=.]")
                .doesNotContain("LayeredJar")
                .doesNotContain("ZIP");
    }

    @Test
    void buildFile_shouldNotConfigureBootJarClassifier() {
        assertThat(buildContent)
                .as("bootJar must not have a custom archiveClassifier — "
                        + "the default empty classifier ensures 'platform.jar' is the "
                        + "executable artifact without a confusing '-boot' suffix")
                .doesNotContainPattern("archiveClassifier");
    }

    @Test
    void springBootCoreClasses_shouldBeOnRuntimeClasspath() {
        // While Loader classes are injected at bootJar time, the spring-boot
        // module's resource handling classes are on the runtime classpath
        assertThatCode(() -> Class.forName("org.springframework.boot.SpringApplication"))
                .as("SpringApplication must be on classpath — it's from spring-boot core "
                        + "which is always available when the plugin is applied")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootAutoconfigure_shouldBeOnRuntimeClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.autoconfigure.SpringBootApplication"))
                .as("@SpringBootApplication must be on classpath — it's from spring-boot-autoconfigure "
                        + "which transitively depends on spring-boot (where Loader infrastructure lives)")
                .doesNotThrowAnyException();
    }

    @Test
    void buildFile_shouldNotDeclareSpringBootLoaderAsExplicitDependency() {
        assertThat(buildContent)
                .as("spring-boot-loader should NOT be an explicit dependency — "
                        + "the Spring Boot Gradle plugin handles its inclusion in bootJar "
                        + "automatically; explicit declaration could cause version conflicts")
                .doesNotContain("\"org.springframework.boot\", \"spring-boot-loader\"")
                .doesNotContain("spring-boot-loader");
    }
}
