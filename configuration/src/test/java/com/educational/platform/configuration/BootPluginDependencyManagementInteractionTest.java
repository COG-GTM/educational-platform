package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates the interaction between the Spring Boot plugin (applied in the
 * configuration module) and the Spring Dependency Management plugin (applied
 * at root level). When both plugins coexist, version management should be
 * exclusively handled by the BOM import — individual dependencies must NOT
 * declare explicit versions (except for libraries not managed by the BOM).
 * <p>
 * Complements {@link PluginCoexistenceValidationTest} which validates plugin
 * application locations, and {@link SpringBootDependencyAlignmentTest} which
 * validates runtime dependency versions.
 */
class BootPluginDependencyManagementInteractionTest {

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
    void starterWeb_shouldNotSpecifyExplicitVersion() {
        String starterWebLine = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-web"))
                .filter(line -> !line.trim().startsWith("//"))
                .findFirst()
                .orElse("");
        assertThat(starterWebLine)
                .as("spring-boot-starter-web must NOT specify an explicit version — "
                        + "version is managed by the BOM imported via dependency-management plugin")
                .doesNotMatch(".*\"\\d+\\.\\d+.*");
    }

    @Test
    void starterTest_shouldNotSpecifyExplicitVersion() {
        String starterTestLine = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .findFirst()
                .orElse("");
        assertThat(starterTestLine)
                .as("spring-boot-starter-test must NOT specify an explicit version — "
                        + "version is managed by the BOM imported via dependency-management plugin")
                .doesNotMatch(".*\"\\d+\\.\\d+.*");
    }

    @Test
    void springBootBomManaged_dependencies_shouldUseTwoArgForm() {
        buildContent.lines()
                .filter(line -> line.contains("org.springframework.boot"))
                .filter(line -> !line.trim().startsWith("//"))
                .forEach(line -> assertThat(line)
                        .as("Spring Boot BOM-managed dependencies should use two-arg form "
                                + "(group, artifact) without version — found: %s", line.trim())
                        .doesNotMatch(".*\"\\d+\\.\\d+\\.\\d+\"\\s*\\)"));
    }

    @Test
    void liquibaseCore_shouldNotSpecifyExplicitVersion() {
        String liquibaseLine = buildContent.lines()
                .filter(line -> line.contains("liquibase-core"))
                .filter(line -> !line.trim().startsWith("//"))
                .findFirst()
                .orElse("");
        assertThat(liquibaseLine)
                .as("liquibase-core must NOT specify an explicit version — "
                        + "its version is managed by the Spring Boot BOM")
                .doesNotMatch(".*\"\\d+\\.\\d+.*");
    }

    @Test
    void runtimeSpringBootVersion_shouldBeConsistentAcrossComponents() {
        String runtimeVersion = SpringBootVersion.getVersion();
        assertThatCode(() -> {
            Class<?> autoConfigClass = Class.forName(
                    "org.springframework.boot.autoconfigure.SpringBootApplication");
            assertThat(autoConfigClass.getPackage().getImplementationVersion())
                    .as("spring-boot-autoconfigure package version should align with spring-boot version")
                    .satisfiesAnyOf(
                            v -> assertThat(v).isEqualTo(runtimeVersion),
                            v -> assertThat(v).isNull() // may be null in dev classpath
                    );
        }).doesNotThrowAnyException();
    }

    @Test
    void buildFile_shouldNotUsePlatformDependency() {
        assertThat(buildContent)
                .as("configuration module must NOT use platform() or enforcedPlatform() — "
                        + "BOM management is handled by the dependency-management plugin at root level")
                .doesNotContain("platform(")
                .doesNotContain("enforcedPlatform(");
    }
}
