package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates that Spring Boot Actuator is NOT on the classpath and not declared
 * in the configuration module's build file. The PR added the Spring Boot plugin
 * and spring-boot-starter-test; if spring-boot-starter-actuator were accidentally
 * added alongside, it would:
 * <ul>
 *   <li>Expose /actuator/* management endpoints without explicit security configuration</li>
 *   <li>Publish health, metrics, and environment details that may leak sensitive info</li>
 *   <li>Add Micrometer and health-indicator auto-configuration overhead to bootRun</li>
 * </ul>
 * Complements {@link SpringBootDevToolsExclusionTest} which guards against DevTools.
 */
class SpringBootActuatorExclusionTest {

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
    void actuatorEndpointDiscoverer_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping"))
                .as("Actuator WebMvcEndpointHandlerMapping must NOT be on the classpath — "
                        + "actuator management endpoints are not needed and would expose "
                        + "health/metrics/env without explicit security configuration")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void actuatorHealthEndpoint_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.boot.actuate.health.HealthEndpoint"))
                .as("Actuator HealthEndpoint must NOT be on the classpath — "
                        + "health indicators are not configured for this deployment model")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void actuatorMetricsEndpoint_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.boot.actuate.metrics.MetricsEndpoint"))
                .as("Actuator MetricsEndpoint must NOT be on the classpath — "
                        + "Micrometer metrics infrastructure is not part of the architecture")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void buildFile_shouldNotDeclareActuatorDependency() {
        assertThat(buildContent)
                .as("build.gradle.kts must not declare spring-boot-starter-actuator — "
                        + "management endpoints should only be added with explicit "
                        + "security configuration for each exposed endpoint")
                .doesNotContain("spring-boot-starter-actuator");
    }

    @Test
    void buildFile_shouldNotDeclareActuatorInAnyScope() {
        assertThat(buildContent)
                .as("build.gradle.kts must not reference 'actuator' in any dependency scope")
                .doesNotContain("actuator");
    }
}
