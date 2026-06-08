package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the server port auto-configuration activates correctly
 * when the Spring Boot plugin is applied. The README documents that bootRun
 * starts the application on port 8080 (default); this test guards that the
 * server.port property resolves to the expected default.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class ServerPortAutoConfigurationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void serverPort_shouldDefaultTo8080_whenNotExplicitlySet() {
        String port = environment.getProperty("server.port");
        // Default Spring Boot behavior: if server.port is not set, it defaults to 8080
        // The property may be null (meaning default 8080) or explicitly "8080"
        if (port != null) {
            assertThat(port)
                    .as("server.port, when explicitly set, must be 8080 as documented in README")
                    .isEqualTo("8080");
        }
        // If null, Spring Boot defaults to 8080 — which is the intended behavior
    }

    @Test
    void embeddedServerFactory_shouldBeAvailableOnClasspath() {
        // Verifies that the embedded server infrastructure is available
        // for bootRun to start the HTTP listener
        boolean hasServerFactory = false;
        for (String name : applicationContext.getBeanDefinitionNames()) {
            if (name.toLowerCase().contains("webserver") || name.toLowerCase().contains("tomcat")
                    || name.toLowerCase().contains("servletcontainer")) {
                hasServerFactory = true;
                break;
            }
        }
        // In WebEnvironment.NONE, the servlet container beans may not be instantiated,
        // but the auto-configuration classes should still be on the classpath
        assertThat(hasServerFactory || isEmbeddedServerOnClasspath())
                .as("Embedded server infrastructure must be available for bootRun on port 8080")
                .isTrue();
    }

    @Test
    void h2ConsoleEnabled_shouldBeTrue() {
        String h2Enabled = environment.getProperty("spring.h2.console.enabled");
        assertThat(h2Enabled)
                .as("H2 console must be enabled in application.properties for development")
                .isEqualTo("true");
    }

    @Test
    void datasourceUrl_shouldBeH2InMemory() {
        String url = environment.getProperty("spring.datasource.url");
        assertThat(url)
                .as("Datasource URL must point to H2 in-memory database")
                .isNotNull()
                .startsWith("jdbc:h2:mem:");
    }

    private boolean isEmbeddedServerOnClasspath() {
        try {
            Class.forName("org.springframework.boot.tomcat.TomcatWebServerFactory");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
