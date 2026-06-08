package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates the Spring Boot Loader classes availability contract when the
 * Spring Boot plugin is applied. The plugin's bootJar task packages the
 * application in a nested-JAR layout (BOOT-INF/) using Spring Boot Loader.
 * The loader classes must be resolvable at build time for the plugin to
 * produce a valid fat JAR, but they should NOT be on the application's
 * runtime classpath during normal execution (they are added by the JAR's
 * manifest Class-Path at launch time).
 * <p>
 * Complements {@link BootJarConfigurationValidationTest} which validates
 * the build DSL, and {@link BootPluginJarTaskConflictTest} which guards
 * against conflicting task configurations.
 */
class BootPluginSpringBootLoaderAvailabilityTest {

    @Test
    void springBootLoaderJarLauncher_shouldNotBeOnTestClasspath() {
        assertThatThrownBy(() -> Class.forName("org.springframework.boot.loader.launch.JarLauncher"))
                .as("Spring Boot Loader JarLauncher must NOT be on the normal classpath — "
                        + "it is only embedded in the fat JAR by the bootJar task and used "
                        + "at launch time via the JAR manifest")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void springBootLoaderWarLauncher_shouldNotBeOnTestClasspath() {
        assertThatThrownBy(() -> Class.forName("org.springframework.boot.loader.launch.WarLauncher"))
                .as("Spring Boot Loader WarLauncher must NOT be on the classpath — "
                        + "the project does not use WAR packaging")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void springBootLoaderPropertiesLauncher_shouldNotBeOnTestClasspath() {
        assertThatThrownBy(() -> Class.forName("org.springframework.boot.loader.launch.PropertiesLauncher"))
                .as("Spring Boot Loader PropertiesLauncher must NOT be on the classpath — "
                        + "it is an alternative launcher for custom classpath layouts")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void springBootStarterWeb_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.web.servlet.DispatcherServlet"))
                .as("DispatcherServlet must be on the classpath — "
                        + "spring-boot-starter-web is an implementation dependency "
                        + "required for bootRun's embedded server")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootAutoConfigure_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.autoconfigure.SpringBootApplication"))
                .as("SpringBootApplication annotation class must be resolvable — "
                        + "the Spring Boot plugin requires it for main class detection")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootVersion_shouldBeResolvable() {
        assertThatCode(() -> {
            Class<?> versionClass = Class.forName("org.springframework.boot.SpringBootVersion");
            Object version = versionClass.getMethod("getVersion").invoke(null);
            assertThat(version).isNotNull();
        })
                .as("SpringBootVersion.getVersion() must be resolvable when the plugin is applied")
                .doesNotThrowAnyException();
    }
}
