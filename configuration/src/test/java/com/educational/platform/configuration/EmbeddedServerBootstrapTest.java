package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring Boot plugin enables full embedded server
 * bootstrap via {@code @SpringBootTest(webEnvironment = RANDOM_PORT)}.
 * Existing tests use {@code NONE} and {@code MOCK}; this test confirms
 * the embedded Tomcat starts on a real port — the same behavior as
 * {@code ./gradlew :configuration:bootRun}. Without the Spring Boot plugin,
 * the context would fail to initialize with web server support.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class EmbeddedServerBootstrapTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void embeddedServer_shouldStartOnRandomPort() {
        assertThat(port)
                .as("Embedded server must bind to a valid port (bootRun starts on 8080, RANDOM_PORT picks an available one)")
                .isGreaterThan(0)
                .isLessThanOrEqualTo(65535);
    }

    @Test
    void applicationContext_shouldBeWebApplicationContext() {
        assertThat(applicationContext)
                .as("Context must be a WebApplicationContext when started with RANDOM_PORT")
                .isInstanceOf(org.springframework.web.context.WebApplicationContext.class);
    }

    @Test
    void servletContext_shouldBeAvailable() {
        var webContext = (org.springframework.web.context.WebApplicationContext) applicationContext;
        assertThat(webContext.getServletContext())
                .as("ServletContext must be available when embedded server is running")
                .isNotNull();
    }

    @Test
    void dispatcherServlet_shouldBeRegistered() {
        assertThat(applicationContext.containsBean("dispatcherServlet"))
                .as("DispatcherServlet must be auto-configured for web request handling")
                .isTrue();
    }

    @Test
    void serverPort_shouldNotBeDefaultBootRunPort() {
        assertThat(port)
                .as("RANDOM_PORT must pick a port different from the default bootRun port 8080 "
                        + "(to avoid conflicts with a running instance)")
                .isNotEqualTo(8080);
    }
}
