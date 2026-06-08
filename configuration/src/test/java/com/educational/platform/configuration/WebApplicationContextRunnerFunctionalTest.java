package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional tests for {@link WebApplicationContextRunner}, the web-aware
 * counterpart of {@link org.springframework.boot.test.context.runner.ApplicationContextRunner}.
 * {@link ApplicationContextRunnerFunctionalTest} covers the non-web runner;
 * this test validates the web variant which creates a
 * {@code GenericWebApplicationContext} with a {@code MockServletContext},
 * essential for testing web auto-configurations in isolation.
 * <p>
 * The Spring Boot plugin enables web auto-configuration (embedded Tomcat,
 * DispatcherServlet); the web runner allows unit-testing those auto-configs
 * without starting a full server, providing faster feedback than
 * {@code @SpringBootTest(webEnvironment = MOCK)}.
 * <p>
 * Complements {@link ApplicationContextRunnerFunctionalTest} (non-web runner)
 * and {@link SpringBootTestMockWebEnvironmentTest} (MOCK mode infrastructure).
 */
class WebApplicationContextRunnerFunctionalTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner();

    @Test
    void webContextRunner_shouldCreateContextWithoutFailure() {
        contextRunner.run(context ->
                assertThat(context).hasNotFailed()
        );
    }

    @Test
    void webContextRunner_shouldProvideServletContext() {
        contextRunner.run(context ->
                assertThat(context.getServletContext())
                        .as("WebApplicationContextRunner must provide a MockServletContext "
                                + "for testing web-specific auto-configuration")
                        .isNotNull()
        );
    }

    @Test
    void webContextRunner_shouldRegisterUserConfiguration() {
        contextRunner
                .withUserConfiguration(WebTestConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(WebTestConfig.class);
                    assertThat(context).hasBean("webTestValue");
                });
    }

    @Test
    void webContextRunner_shouldSupportPropertyOverrides() {
        contextRunner
                .withPropertyValues("server.servlet.context-path=/api")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment()
                            .getProperty("server.servlet.context-path"))
                            .isEqualTo("/api");
                });
    }

    @Test
    void webContextRunner_shouldSupportAutoConfiguration() {
        contextRunner
                .withPropertyValues("spring.main.web-application-type=servlet")
                .run(context ->
                        assertThat(context).hasNotFailed()
                );
    }

    @Configuration
    static class WebTestConfig {
        @Bean
        String webTestValue() {
            return "web-test";
        }
    }
}
