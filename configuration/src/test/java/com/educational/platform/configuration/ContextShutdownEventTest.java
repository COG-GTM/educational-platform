package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that ContextClosedEvent is published during application context
 * shutdown. bootRun relies on graceful shutdown (Ctrl+C sends SIGTERM) which
 * triggers ContextClosedEvent for resource cleanup — database connections,
 * thread pools, Liquibase locks. SpringBootApplicationEventsTest covers
 * startup events; this test covers the shutdown side of the lifecycle.
 */
class ContextShutdownEventTest {

    private static final String ISOLATED_DB = "--spring.datasource.url=jdbc:h2:mem:shutdown_test;DB_CLOSE_DELAY=-1";

    @Test
    void contextClosedEvent_shouldBePublished_onContextClose() {
        List<String> receivedEvents = new ArrayList<>();

        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);

        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        context.addApplicationListener(event -> {
            if (event instanceof ContextClosedEvent) {
                receivedEvents.add("ContextClosed");
            }
        });

        context.close();

        assertThat(receivedEvents)
                .as("ContextClosedEvent must be published during shutdown for resource cleanup")
                .contains("ContextClosed");
    }

    @Test
    void contextClosedEvent_shouldFireExactlyOnce_onSingleClose() {
        List<String> receivedEvents = new ArrayList<>();

        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);

        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        context.addApplicationListener(event -> {
            if (event instanceof ContextClosedEvent) {
                receivedEvents.add("ContextClosed");
            }
        });

        context.close();

        assertThat(receivedEvents)
                .as("ContextClosedEvent must fire exactly once per context close")
                .hasSize(1);
    }

    @Test
    void contextClose_shouldBeIdempotent() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);

        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        context.close();
        // Second close should not throw
        context.close();

        assertThat(context.isActive())
                .as("Context must not be active after double close")
                .isFalse();
    }

    @Test
    void contextClose_shouldCompleteWithoutException() {
        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);

        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        assertThat(context.isActive()).isTrue();

        context.close();

        assertThat(context.isActive())
                .as("Context must transition from active to inactive on shutdown")
                .isFalse();
    }
}
