package com.educational.platform.courses.course.rating.update;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CourseRatingRecalculatedIntegrationEventHandlerRetryTest {

    @EnableRetry
    @Configuration
    static class RetryConfig {
    }

    @Test
    void handleCourseRatingRecalculatedEvent_commandHandlerAlwaysFails_retriedThreeTimesThenRecovered() {
        final UpdateCourseRatingCommandHandler commandHandler = mock(UpdateCourseRatingCommandHandler.class);
        doThrow(new RuntimeException("boom")).when(commandHandler).handle(any());
        final CourseRatingRecalculatedIntegrationEventHandler target = spy(new CourseRatingRecalculatedIntegrationEventHandler(commandHandler));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RetryConfig.class);
            context.registerBean(CourseRatingRecalculatedIntegrationEventHandler.class, () -> target);
            context.refresh();

            final CourseRatingRecalculatedIntegrationEventHandler sut = context.getBean(CourseRatingRecalculatedIntegrationEventHandler.class);
            final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), 4.5);

            sut.handleCourseRatingRecalculatedEvent(event);

            verify(commandHandler, times(3)).handle(any());
            verify(target).recover(any(RuntimeException.class), any(CourseRatingRecalculatedIntegrationEvent.class));
        }
    }

}
