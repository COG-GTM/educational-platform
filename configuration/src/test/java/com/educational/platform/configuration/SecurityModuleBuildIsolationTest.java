package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the invariant that security modules must NOT apply the Spring Boot
 * plugin. {@link LibraryModuleBuildIsolationTest} covers domain modules
 * (courses, users, etc.) but omits the security subprojects. The security
 * modules provide a starter-security dependency and test utilities — applying
 * the Spring Boot plugin would cause them to produce fat JARs, register
 * bootRun tasks, and potentially trigger ambiguous main-class detection,
 * breaking the configuration module's sole-entry-point contract.
 */
class SecurityModuleBuildIsolationTest {

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
            "security/config/build.gradle.kts",
            "security/test/build.gradle.kts"
    })
    void securityModule_shouldNotApplySpringBootPlugin(String buildFilePath) throws IOException {
        Path buildFile = projectRoot.resolve(buildFilePath);
        if (!Files.exists(buildFile)) {
            return;
        }
        String content = Files.readString(buildFile);
        assertThat(content)
                .as("Module '%s' must NOT apply the Spring Boot plugin via alias — "
                        + "only the configuration module should apply it", buildFilePath)
                .doesNotContain("libs.plugins.springboot");
        assertThat(content)
                .as("Module '%s' must NOT apply the Spring Boot plugin via legacy syntax",
                        buildFilePath)
                .doesNotContainPattern("apply.*plugin.*org\\.springframework\\.boot");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "security/config/build.gradle.kts",
            "security/test/build.gradle.kts"
    })
    void securityModule_shouldNotDeclarePluginsBlock(String buildFilePath) throws IOException {
        Path buildFile = projectRoot.resolve(buildFilePath);
        if (!Files.exists(buildFile)) {
            return;
        }
        String content = Files.readString(buildFile);
        assertThat(content)
                .as("Module '%s' must NOT have a plugins {} block — "
                        + "security modules are plain library modules inheriting "
                        + "java and dependency-management from the root build", buildFilePath)
                .doesNotContainPattern(
                        java.util.regex.Pattern.compile("^plugins\\s*\\{",
                                java.util.regex.Pattern.MULTILINE));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "security/config/build.gradle.kts",
            "security/test/build.gradle.kts"
    })
    void securityModule_shouldNotDeclareBootRunOrBootJarTasks(String buildFilePath)
            throws IOException {
        Path buildFile = projectRoot.resolve(buildFilePath);
        if (!Files.exists(buildFile)) {
            return;
        }
        String content = Files.readString(buildFile);
        assertThat(content)
                .as("Module '%s' must NOT configure bootRun — "
                        + "only the configuration module owns the bootRun task", buildFilePath)
                .doesNotContain("bootRun");
        assertThat(content)
                .as("Module '%s' must NOT configure bootJar — "
                        + "only the configuration module owns the bootJar task", buildFilePath)
                .doesNotContain("bootJar");
    }
}
