package com.educational.platform.courses.course.rating.update;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CourseRatingRecalculatedIntegrationEventHandlerRetryTest {

    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableResilientMethods
    static class RetryTestConfiguration {

        @Bean
        CourseRatingRecalculatedIntegrationEventHandler courseRatingRecalculatedIntegrationEventHandler(UpdateCourseRatingCommandHandler handler) {
            return new CourseRatingRecalculatedIntegrationEventHandler(handler);
        }

    }

    @BeforeEach
    void setUp() {
        updateCourseRatingCommandHandler = mock(UpdateCourseRatingCommandHandler.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(UpdateCourseRatingCommandHandler.class, () -> updateCourseRatingCommandHandler);
        context.register(RetryTestConfiguration.class);
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void handleCourseRatingRecalculatedEvent_transientFailure_retriedUntilSuccess() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new RuntimeException("transient failure"))
                .doThrow(new RuntimeException("transient failure"))
                .doNothing()
                .when(updateCourseRatingCommandHandler).handle(any(UpdateCourseRatingCommand.class));
        final CourseRatingRecalculatedIntegrationEventHandler sut = context.getBean(CourseRatingRecalculatedIntegrationEventHandler.class);

        // when
        sut.handleCourseRatingRecalculatedEvent(new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5));

        // then
        verify(updateCourseRatingCommandHandler, times(3)).handle(new UpdateCourseRatingCommand(uuid, 4.5));
    }

    @Test
    void handleCourseRatingRecalculatedEvent_persistentFailure_retriesExhaustedAndExceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new RuntimeException("persistent failure"))
                .when(updateCourseRatingCommandHandler).handle(any(UpdateCourseRatingCommand.class));
        final CourseRatingRecalculatedIntegrationEventHandler sut = context.getBean(CourseRatingRecalculatedIntegrationEventHandler.class);

        // when // then
        assertThrows(RuntimeException.class,
                () -> sut.handleCourseRatingRecalculatedEvent(new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5)));
        verify(updateCourseRatingCommandHandler, times(4)).handle(any(UpdateCourseRatingCommand.class));
    }

    @Test
    void publishEventWithoutTransaction_fallbackExecution_handlerInvoked() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doNothing().when(updateCourseRatingCommandHandler).handle(any(UpdateCourseRatingCommand.class));

        // when
        context.publishEvent(new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5));

        // then
        verify(updateCourseRatingCommandHandler).handle(new UpdateCourseRatingCommand(uuid, 4.5));
    }

}
