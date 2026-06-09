package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the invariant that integration-events modules and infrastructure
 * modules do NOT declare {@code spring-boot-starter-web}. In the modular
 * monolith architecture, starter-web is appropriate in application and web
 * layers (which define REST controllers) and in the configuration module
 * (composition root). However, integration-events modules only define domain
 * event classes and must NOT depend on the web stack:
 * <ul>
 *   <li>Integration-events modules are shared across bounded contexts and
 *       adding web dependencies would pollute their classpath</li>
 *   <li>The common module provides shared utilities and should not depend
 *       on embedded Tomcat</li>
 *   <li>Security modules provide auth configuration and should not declare
 *       their own web starter</li>
 * </ul>
 * Complements {@link StarterTestModuleIsolationTest} which guards
 * starter-test scope, and {@link LibraryModuleBuildIsolationTest} which
 * guards against the Spring Boot plugin in library modules.
 */
class DomainModuleStarterWebIsolationTest {

    private static Path projectRoot;

    @BeforeAll
    static void findProjectRoot() {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "courses/integration-events/build.gradle.kts",
            "administration/integration-events/build.gradle.kts",
            "course-enrollments/integration-events/build.gradle.kts",
            "course-reviews/integration-events/build.gradle.kts",
            "users/integration-events/build.gradle.kts",
            "common/build.gradle.kts"
    })
    void integrationEventsModule_shouldNotDeclareStarterWeb(String buildFilePath)
            throws IOException {
        Path buildFile = projectRoot.resolve(buildFilePath);
        if (!Files.exists(buildFile)) {
            return;
        }
        String content = Files.readString(buildFile);
        assertThat(content)
                .as("Module '%s' must NOT declare spring-boot-starter-web — "
                        + "integration-events modules define domain event classes only "
                        + "and must not depend on the web stack", buildFilePath)
                .doesNotContain("spring-boot-starter-web");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "security/config/build.gradle.kts",
            "security/test/build.gradle.kts"
    })
    void securityModule_shouldNotDeclareStarterWeb(String buildFilePath) throws IOException {
        Path buildFile = projectRoot.resolve(buildFilePath);
        if (!Files.exists(buildFile)) {
            return;
        }
        String content = Files.readString(buildFile);
        assertThat(content)
                .as("Module '%s' must NOT declare spring-boot-starter-web — "
                        + "security modules provide auth configuration and should not "
                        + "own embedded web server dependencies", buildFilePath)
                .doesNotContain("spring-boot-starter-web");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "courses/integration-events/build.gradle.kts",
            "administration/integration-events/build.gradle.kts",
            "course-enrollments/integration-events/build.gradle.kts",
            "course-reviews/integration-events/build.gradle.kts",
            "users/integration-events/build.gradle.kts",
            "common/build.gradle.kts"
    })
    void eventModule_shouldNotDeclareBareSpringBootStarter(String buildFilePath)
            throws IOException {
        Path buildFile = projectRoot.resolve(buildFilePath);
        if (!Files.exists(buildFile)) {
            return;
        }
        String content = Files.readString(buildFile);
        assertThat(content.lines()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("\"spring-boot-starter\""))
                .filter(line -> !line.contains("spring-boot-starter-"))
                .toList())
                .as("Module '%s' must NOT declare the bare spring-boot-starter — "
                        + "use specific starters to make dependency intent explicit", buildFilePath)
                .isEmpty();
    }

    @Test
    void configurationModule_shouldDeclareStarterWeb() throws IOException {
        String configContent = Files.readString(
                projectRoot.resolve("configuration/build.gradle.kts"));
        assertThat(configContent)
                .as("configuration/build.gradle.kts must declare spring-boot-starter-web — "
                        + "the configuration module is the composition root and HTTP entry point")
                .contains("spring-boot-starter-web");
    }

    @Test
    void rootBuild_shouldNotDeclareStarterWeb() throws IOException {
        String rootContent = Files.readString(projectRoot.resolve("build.gradle.kts"));
        assertThat(rootContent)
                .as("Root build.gradle.kts must NOT declare spring-boot-starter-web — "
                        + "it should not be applied globally via allprojects or subprojects")
                .doesNotContain("spring-boot-starter-web");
    }

    @Test
    void rootBuild_shouldNotDeclareAnyStarter() throws IOException {
        String rootContent = Files.readString(projectRoot.resolve("build.gradle.kts"));
        assertThat(rootContent.lines()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("spring-boot-starter"))
                .toList())
                .as("Root build.gradle.kts must NOT declare any Spring Boot starter — "
                        + "starter dependencies belong in individual module build files")
                .isEmpty();
    }
}
