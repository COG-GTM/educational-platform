package com.educational.platform.courses.course.rating.update;

import com.educational.platform.common.event.FailedIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Exercises the declarative {@code @Retryable}/{@code @Recover} behaviour of
 * {@link CourseRatingRecalculatedIntegrationEventHandler} in a Spring context with retry enabled.
 */
@SpringJUnitConfig
class CourseRatingRecalculatedIntegrationEventHandlerRetryTest {

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler() {
            return mock(UpdateCourseRatingCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedEventRepository() {
            return mock(FailedIntegrationEventRepository.class);
        }

        @Bean
        CourseRatingRecalculatedIntegrationEventHandler handler(
                UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler,
                FailedIntegrationEventRepository failedEventRepository) {
            return new CourseRatingRecalculatedIntegrationEventHandler(
                    updateCourseRatingCommandHandler, failedEventRepository);
        }
    }

    @Autowired
    private CourseRatingRecalculatedIntegrationEventHandler handler;

    @Autowired
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @Autowired
    private FailedIntegrationEventRepository failedEventRepository;

    @BeforeEach
    void resetMocks() {
        reset(updateCourseRatingCommandHandler, failedEventRepository);
    }

    @Test
    void transientFailure_retriesThreeTimesThenDeadLetters() {
        // given
        final CourseRatingRecalculatedIntegrationEvent event =
                new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), 3.7);
        doThrow(new OptimisticLockingFailureException("x"))
                .when(updateCourseRatingCommandHandler).handle(any(UpdateCourseRatingCommand.class));

        // when
        handler.handleCourseRatingRecalculatedEvent(event);

        // then
        verify(updateCourseRatingCommandHandler, times(3)).handle(any(UpdateCourseRatingCommand.class));
        verify(failedEventRepository, times(1)).save(any(FailedIntegrationEvent.class));
    }

    @Test
    void businessFailure_isNotRetried() {
        // given
        final CourseRatingRecalculatedIntegrationEvent event =
                new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), 3.7);
        doThrow(new ResourceNotFoundException("x"))
                .when(updateCourseRatingCommandHandler).handle(any(UpdateCourseRatingCommand.class));

        // when
        handler.handleCourseRatingRecalculatedEvent(event);

        // then: business exception is attempted exactly once (not retried), then dead-lettered
        verify(updateCourseRatingCommandHandler, times(1)).handle(any(UpdateCourseRatingCommand.class));
        verify(failedEventRepository, times(1)).save(any(FailedIntegrationEvent.class));
    }
}
