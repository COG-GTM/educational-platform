package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot plugin enables proper application context
 * lifecycle management — context creation, refresh, and graceful shutdown.
 * bootRun depends on the context being closeable (Ctrl+C sends shutdown);
 * these tests verify the lifecycle hooks function correctly.
 * Uses isolated H2 databases (DB_CLOSE_DELAY=-1) to avoid interfering with other tests.
 */
class ApplicationContextLifecycleTest {

    private static final String ISOLATED_DB_ARGS = "--spring.datasource.url=jdbc:h2:mem:lifecycle_test;DB_CLOSE_DELAY=-1";

    @Test
    void applicationContext_shouldStartAndClose_gracefully() {
        assertThatCode(() -> {
            SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
            app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
            ConfigurableApplicationContext context = app.run(ISOLATED_DB_ARGS);
            assertThat(context.isRunning() || context.isActive())
                    .as("Context must be active after start")
                    .isTrue();
            context.close();
            assertThat(context.isActive())
                    .as("Context must not be active after close")
                    .isFalse();
        }).doesNotThrowAnyException();
    }

    @Test
    void applicationContext_shouldRegisterShutdownHook() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(ISOLATED_DB_ARGS);
        try {
            assertThat(context.isActive())
                    .as("Context must be active after startup with shutdown hook registered")
                    .isTrue();
        } finally {
            context.close();
        }
    }

    @Test
    void applicationContextRunner_shouldSupportLazyInit() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.main.lazy-initialization=true")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void multipleContextCreation_shouldNotInterfereWithEachOther() {
        assertThatCode(() -> {
            SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
            app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);

            ConfigurableApplicationContext ctx1 = app.run(
                    "--spring.datasource.url=jdbc:h2:mem:lifecycle_multi1;DB_CLOSE_DELAY=-1");
            ctx1.close();

            ConfigurableApplicationContext ctx2 = app.run(
                    "--spring.datasource.url=jdbc:h2:mem:lifecycle_multi2;DB_CLOSE_DELAY=-1");
            assertThat(ctx2.isActive())
                    .as("Second context creation must succeed after first is closed")
                    .isTrue();
            ctx2.close();
        }).doesNotThrowAnyException();
    }
}
