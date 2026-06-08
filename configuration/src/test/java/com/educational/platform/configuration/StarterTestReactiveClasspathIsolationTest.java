package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates that Spring WebFlux and reactive-stack classes are NOT on the
 * test classpath. The configuration module uses {@code spring-boot-starter-web}
 * (servlet-based); the Spring Boot plugin auto-detects the web application
 * type based on classpath presence of WebFlux classes. If reactive classes
 * leaked onto the classpath via starter-test or another transitive, the
 * boot plugin could incorrectly configure a reactive web environment,
 * causing bootRun to start a Netty server instead of Tomcat.
 * <p>
 * Complements {@link StarterTestTransitiveSafetyTest} (DevTools, Selenium,
 * JUnit 4 exclusion) and {@link StarterTestTransitiveDependencyPresenceTest}
 * (required transitives).
 */
class StarterTestReactiveClasspathIsolationTest {

    @Test
    void webFluxAutoConfiguration_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration"))
                .as("WebFluxAutoConfiguration must NOT be on the classpath — "
                        + "its presence would cause Spring Boot to auto-configure a reactive "
                        + "web server instead of the expected servlet-based Tomcat")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void reactiveWebServerFactory_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.boot.web.reactive.server.ReactiveWebServerFactory"))
                .as("ReactiveWebServerFactory must NOT be on the classpath — "
                        + "the project uses servlet-based deployment via Tomcat")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void webFluxConfigurer_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.web.reactive.config.WebFluxConfigurer"))
                .as("WebFluxConfigurer must NOT be on the classpath — "
                        + "reactive web configuration interfaces should not be available "
                        + "in a servlet-based project")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void reactorCore_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName("reactor.core.publisher.Mono"))
                .as("Project Reactor (Mono) must NOT be on the classpath — "
                        + "the project is servlet-based and does not use reactive streams")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void reactorNetty_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName(
                "reactor.netty.http.server.HttpServer"))
                .as("Reactor Netty must NOT be on the classpath — "
                        + "the project uses Tomcat as the embedded server, not Netty")
                .isInstanceOf(ClassNotFoundException.class);
    }
}
