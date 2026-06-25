package com.educational.platform.courses.course.rating.update;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Spring Retry integration test for {@link CourseRatingRecalculatedIntegrationEventHandler}.
 * Validates retry/recover with an event containing both UUID and double payload fields.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CourseRatingRecalculatedRetryIntegrationTest.RetryTestConfig.class)
class CourseRatingRecalculatedRetryIntegrationTest {

    @Autowired
    private CourseRatingRecalculatedIntegrationEventHandler handler;

    @Autowired
    private UpdateCourseRatingCommandHandler commandHandler;

    @Autowired
    private FailedIntegrationEventRepository failedEventRepository;

    @Autowired
    private AtomicInteger invocationCounter;

    @BeforeEach
    void setUp() {
        Mockito.reset(commandHandler, failedEventRepository);
        invocationCounter.set(0);
    }

    @Test
    void retryable_retriesAndRecovers_eventPayloadContainsBothCourseIdAndRating() {
        // given
        final UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.75);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("timeout during rating update");
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseRatingRecalculatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        FailedIntegrationEventRecord record = captor.getValue();
        assertThat(record.getEventPayload()).contains("550e8400-e29b-41d4-a716-446655440000");
        assertThat(record.getEventPayload()).contains("4.75");
        assertThat(record.getExceptionMessage()).isEqualTo("timeout during rating update");
    }

    @Test
    void retryable_withNegativeRating_retriesAndRecoverCorrectly() {
        // given - negative rating is a valid domain value
        final UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, -1.5);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new DeadlockLoserDataAccessException("deadlock", null);
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseRatingRecalculatedEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventPayload()).contains("-1.5");
    }

    @Test
    void retryable_succeedsFirstTime_noRetryNoRecover() {
        // given
        final UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 5.0);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            return null;
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseRatingRecalculatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_nonRetryableException_propagatesImmediately() {
        // given
        final UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.0);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new UnsupportedOperationException("not implemented");
        }).when(commandHandler).handle(any());

        // when / then - non-retryable wraps in ExhaustedRetryException
        assertThatThrownBy(() -> handler.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_commandHandlerReceivesCorrectRatingOnEachRetry() {
        // given
        final UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440004");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 2.5);
        var ratingCaptor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            if (invocationCounter.get() < 3) {
                throw new OptimisticLockingFailureException("retry");
            }
            return null;
        }).when(commandHandler).handle(ratingCaptor.capture());

        // when
        handler.handleCourseRatingRecalculatedEvent(event);

        // then - each retry receives the same event data
        assertThat(ratingCaptor.getAllValues()).hasSize(3);
        ratingCaptor.getAllValues().forEach(cmd ->
                assertThat(cmd).hasFieldOrPropertyWithValue("rating", 2.5)
        );
    }

    @Configuration
    @EnableRetry
    static class RetryTestConfig {

        @Bean
        AtomicInteger invocationCounter() {
            return new AtomicInteger(0);
        }

        @Bean
        UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler() {
            return mock(UpdateCourseRatingCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedIntegrationEventRepository() {
            return mock(FailedIntegrationEventRepository.class);
        }

        @Bean
        CourseRatingRecalculatedIntegrationEventHandler courseRatingRecalculatedIntegrationEventHandler(
                UpdateCourseRatingCommandHandler commandHandler,
                FailedIntegrationEventRepository repository) {
            return new CourseRatingRecalculatedIntegrationEventHandler(commandHandler, repository);
        }
    }
}
