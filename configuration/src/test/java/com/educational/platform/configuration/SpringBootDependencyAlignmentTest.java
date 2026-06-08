package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates alignment between the Spring Boot plugin version (applied via
 * libs.plugins.springboot in build.gradle.kts) and the Spring Boot runtime
 * dependencies managed by the BOM. Misalignment would cause classpath
 * conflicts or missing method errors at runtime.
 */
class SpringBootDependencyAlignmentTest {

    @Test
    void springBootStarterWeb_shouldBeAvailableOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.web.servlet.support.SpringBootServletInitializer"))
                .as("spring-boot-starter-web classes must be resolvable, confirming dependency management alignment")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootAutoConfigure_shouldBeAvailableOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.autoconfigure.SpringBootApplication"))
                .as("spring-boot-autoconfigure must be on classpath for auto-configuration to work")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootActuator_classVersion_shouldMatchPluginVersion() {
        // SpringBootVersion reflects the actual runtime version of the BOM-managed dependencies
        String runtimeVersion = SpringBootVersion.getVersion();
        assertThat(runtimeVersion)
                .as("Runtime Spring Boot version must start with the same major.minor as declared in libs.versions.toml")
                .startsWith("4.0");
    }

    @Test
    void springFramework_shouldBeCompatibleVersion() {
        // Spring Framework version must be compatible with Spring Boot 4.x (requires Spring Framework 7.x)
        String springVersion = org.springframework.core.SpringVersion.getVersion();
        assertThat(springVersion)
                .as("Spring Framework version must be 7.x for compatibility with Spring Boot 4.x")
                .startsWith("7.");
    }

    @Test
    void embeddedServletContainer_shouldBeAvailable() {
        assertThatCode(() -> Class.forName("org.springframework.boot.tomcat.TomcatWebServerFactory"))
                .as("Embedded Tomcat must be available via spring-boot-starter-web for bootRun")
                .doesNotThrowAnyException();
    }

    @Test
    void springDataJpa_shouldBeAvailable() {
        assertThatCode(() -> Class.forName("org.springframework.data.jpa.repository.JpaRepository"))
                .as("Spring Data JPA must be available for repository-based persistence")
                .doesNotThrowAnyException();
    }

    @Test
    void h2Database_shouldBeAvailable() {
        assertThatCode(() -> Class.forName("org.h2.Driver"))
                .as("H2 driver must be available for in-memory database testing")
                .doesNotThrowAnyException();
    }
}
