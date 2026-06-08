package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates that Spring Boot DevTools is intentionally excluded from both
 * the production runtime classpath and the test classpath. DevTools enables
 * automatic restart, live reload, and relaxed security settings that are
 * inappropriate for production-grade deployment via bootRun/bootJar.
 * Including DevTools would cause:
 * - Unexpected application restarts during integration tests
 * - Relaxed security defaults that weaken Spring Security configuration
 * - Class-loading issues with the restart classloader in production
 */
class SpringBootDevToolsExclusionTest {

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
    void devToolsRestarter_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName("org.springframework.boot.devtools.restart.Restarter"))
                .as("DevTools Restarter must NOT be on the classpath — "
                        + "automatic restart is inappropriate for production bootRun")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void devToolsLiveReload_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName("org.springframework.boot.devtools.livereload.LiveReloadServer"))
                .as("DevTools LiveReload must NOT be on the classpath — "
                        + "live reload is a development-only feature")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void devToolsRemote_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName("org.springframework.boot.devtools.remote.client.RemoteClientConfiguration"))
                .as("DevTools remote client must NOT be on the classpath — "
                        + "remote debugging infrastructure is not needed")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void buildFile_shouldNotDeclareDevelopmentOnlyDependency() {
        assertThat(buildContent)
                .as("build.gradle.kts must not use developmentOnly scope for any dependency")
                .doesNotContain("developmentOnly(");
    }

    @Test
    void buildFile_shouldNotReferenceDevToolsDependency() {
        assertThat(buildContent)
                .as("build.gradle.kts must not declare spring-boot-devtools as a dependency in any scope")
                .doesNotContain("spring-boot-devtools");
    }

    @Test
    void devToolsPropertyDefaults_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName("org.springframework.boot.devtools.env.DevToolsPropertyDefaultsPostProcessor"))
                .as("DevTools property defaults processor must NOT be on the classpath — "
                        + "it overrides security and caching settings for development convenience")
                .isInstanceOf(ClassNotFoundException.class);
    }
}
