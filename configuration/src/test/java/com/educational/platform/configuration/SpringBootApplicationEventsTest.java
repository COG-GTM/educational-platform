package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring Boot plugin enables proper application lifecycle
 * event publishing. bootRun relies on these events for startup hooks, health
 * indicators, and inter-module initialisation. Without the plugin, the context
 * cannot bootstrap and these events would never fire.
 */
class SpringBootApplicationEventsTest {

    private static final String ISOLATED_DB = "--spring.datasource.url=jdbc:h2:mem:events_test;DB_CLOSE_DELAY=-1";

    @Test
    void applicationStartedEvent_shouldBePublished_afterContextRefresh() {
        List<String> receivedEvents = new ArrayList<>();

        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.addListeners(event -> {
            if (event instanceof ApplicationStartedEvent) {
                receivedEvents.add("ApplicationStartedEvent");
            }
        });

        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        try {
            assertThat(receivedEvents)
                    .as("ApplicationStartedEvent must be published during boot — required for startup hooks")
                    .contains("ApplicationStartedEvent");
        } finally {
            context.close();
        }
    }

    @Test
    void applicationReadyEvent_shouldBePublished_afterStartedEvent() {
        List<String> receivedEvents = new ArrayList<>();

        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.addListeners(event -> {
            if (event instanceof ApplicationStartedEvent) {
                receivedEvents.add("Started");
            } else if (event instanceof ApplicationReadyEvent) {
                receivedEvents.add("Ready");
            }
        });

        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        try {
            assertThat(receivedEvents)
                    .as("Both ApplicationStartedEvent and ApplicationReadyEvent must fire in order")
                    .containsExactly("Started", "Ready");
        } finally {
            context.close();
        }
    }

    @Test
    void contextRefreshedEvent_shouldBePublished_duringStartup() {
        List<String> receivedEvents = new ArrayList<>();

        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.addListeners(event -> {
            if (event instanceof ContextRefreshedEvent) {
                receivedEvents.add("ContextRefreshed");
            }
        });

        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        try {
            assertThat(receivedEvents)
                    .as("ContextRefreshedEvent must fire during startup for eager bean initialization")
                    .contains("ContextRefreshed");
        } finally {
            context.close();
        }
    }

    @Test
    void eventOrdering_shouldBeContextRefreshed_thenStarted_thenReady() {
        List<String> receivedEvents = new ArrayList<>();

        SpringApplication app = new SpringApplication(EducationalPlatformApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.addListeners(event -> {
            if (event instanceof ContextRefreshedEvent) {
                receivedEvents.add("Refreshed");
            } else if (event instanceof ApplicationStartedEvent) {
                receivedEvents.add("Started");
            } else if (event instanceof ApplicationReadyEvent) {
                receivedEvents.add("Ready");
            }
        });

        ConfigurableApplicationContext context = app.run(ISOLATED_DB);
        try {
            assertThat(receivedEvents)
                    .as("Spring Boot event order must be: ContextRefreshed → Started → Ready")
                    .containsSubsequence("Refreshed", "Started", "Ready");
        } finally {
            context.close();
        }
    }
}
