package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the {@code @RecordApplicationEvents} API from the Spring
 * TestContext Framework. This annotation, combined with the
 * {@link ApplicationEvents} interface, allows tests to capture and assert
 * on application events published during a test method — a key testing
 * pattern for the modular monolith's inter-module event communication.
 * <p>
 * {@link SpringBootApplicationEventsTest} validates lifecycle events
 * (Started, Ready) via manual listener registration on {@code SpringApplication}.
 * This test validates the declarative test-infrastructure approach where events
 * are captured per-test-method and injected via {@code @Autowired}. The
 * {@code @RecordApplicationEvents} feature requires the Spring TestContext
 * Framework from {@code spring-test} (transitively provided by
 * {@code spring-boot-starter-test}).
 * <p>
 * Note: {@code @RecordApplicationEvents} captures events published during
 * the test method execution, not during context startup. Context lifecycle
 * events (e.g., {@code ContextRefreshedEvent}) fire before recording begins
 * and are validated by {@link SpringBootApplicationEventsTest} instead.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@RecordApplicationEvents
class SpringBootTestRecordApplicationEventsTest {

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    void applicationEvents_shouldBeInjectable() {
        assertThat(applicationEvents)
                .as("ApplicationEvents must be injectable via @Autowired when "
                        + "@RecordApplicationEvents is present on the test class")
                .isNotNull();
    }

    @Test
    void applicationEvents_shouldCaptureCustomPublishedEvents() {
        record TestDomainEvent(String message) {}
        TestDomainEvent event = new TestDomainEvent("module-integration-check");
        eventPublisher.publishEvent(event);

        long count = applicationEvents.stream(TestDomainEvent.class).count();
        assertThat(count)
                .as("Events published via ApplicationEventPublisher during a test method "
                        + "must be captured by @RecordApplicationEvents — this is critical "
                        + "for testing inter-module event communication in the modular monolith")
                .isEqualTo(1);
    }

    @Test
    void applicationEvents_shouldSupportStreamFiltering() {
        record FilterableEvent(String type) {}
        eventPublisher.publishEvent(new FilterableEvent("A"));
        eventPublisher.publishEvent(new FilterableEvent("B"));

        long count = applicationEvents.stream(FilterableEvent.class)
                .filter(e -> "A".equals(e.type()))
                .count();
        assertThat(count)
                .as("ApplicationEvents.stream() must support standard Stream filtering")
                .isEqualTo(1);
    }
}
