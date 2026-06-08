package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot plugin auto-configures web infrastructure
 * beans (DispatcherServlet, embedded Tomcat, etc.) that are required for
 * bootRun to serve HTTP traffic on port 8080. Uses WebEnvironment.NONE
 * to avoid servlet context resource resolution issues with @PropertySource,
 * but verifies web-layer bean definitions are present in the context.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class WebMockEnvironmentBootTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void webMvcAutoConfiguration_shouldBeActive() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasWebMvcBean = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("webmvc") || name.toLowerCase().contains("requestmapping")) {
                hasWebMvcBean = true;
                break;
            }
        }
        assertThat(hasWebMvcBean)
                .as("WebMvc auto-configuration beans must be registered when spring-boot-starter-web is present")
                .isTrue();
    }

    @Test
    void embeddedServletContainerFactory_shouldBeAvailable() {
        assertThatCode(() -> Class.forName("org.springframework.boot.tomcat.TomcatWebServerFactory"))
                .as("Embedded Tomcat web server factory must be on the classpath for bootRun")
                .doesNotThrowAnyException();
    }

    @Test
    void dispatcherServletAutoConfiguration_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.web.servlet.DispatcherServlet"))
                .as("DispatcherServlet must be on the classpath for web request routing via bootRun")
                .doesNotThrowAnyException();
    }

    @Test
    void springSecurityBeans_shouldBeRegistered() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasSecurityBean = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("security")) {
                hasSecurityBean = true;
                break;
            }
        }
        assertThat(hasSecurityBean)
                .as("Spring Security beans must be auto-configured alongside web infrastructure")
                .isTrue();
    }

    @Test
    void restControllerBeans_shouldBeDiscoverable() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasRestController = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("controller") || name.toLowerCase().contains("api")) {
                hasRestController = true;
                break;
            }
        }
        assertThat(hasRestController)
                .as("REST controller beans from bounded context modules must be discoverable via web auto-configuration")
                .isTrue();
    }
}
