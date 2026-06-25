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
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
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
 * Additional Spring Retry edge-case integration tests for {@link CourseRatingRecalculatedIntegrationEventHandler}.
 * Covers exception transition scenarios (retryable -> non-retryable on subsequent attempts)
 * and deep exception hierarchy subclasses that complement the base {@link CourseRatingRecalculatedRetryIntegrationTest}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CourseRatingRecalculatedRetryEdgeCaseTest.RetryTestConfig.class)
class CourseRatingRecalculatedRetryEdgeCaseTest {

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
    void retryable_retryableExceptionThenNonRetryable_stopsRetryAndPropagates() {
        // given - first attempt throws retryable, second throws non-retryable
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) {
                throw new QueryTimeoutException("transient on first attempt");
            }
            throw new IllegalArgumentException("invalid rating on second attempt");
        }).when(commandHandler).handle(any());

        // when / then - Spring Retry retries after first (retryable) exception,
        // but second (non-retryable) has no matching @Recover, so wraps in ExhaustedRetryException
        assertThatThrownBy(() -> handler.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);

        assertThat(invocationCounter.get()).isEqualTo(2);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_retryableExceptionsThenNonRetryableOnLastAttempt_propagatesWithoutRecover() {
        // given - two retryable exceptions, then non-retryable on final attempt
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count <= 2) {
                throw new OptimisticLockingFailureException("retryable attempt " + count);
            }
            throw new NullPointerException("null on final attempt");
        }).when(commandHandler).handle(any());

        // when / then - all 3 attempts used; last throws non-retryable, no matching @Recover
        assertThatThrownBy(() -> handler.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(NullPointerException.class);

        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_cannotAcquireLockException_retriesAndRecovers() {
        // given - CannotAcquireLockException extends PessimisticLockingFailureException
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.0);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new CannotAcquireLockException("lock wait timeout exceeded");
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseRatingRecalculatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("lock wait timeout exceeded");
    }

    @Test
    void retryable_deadlockLoserException_retriesAndRecovers() {
        // given - DeadlockLoserDataAccessException extends PessimisticLockingFailureException
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440004");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 2.5);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new DeadlockLoserDataAccessException("deadlock victim", null);
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseRatingRecalculatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("deadlock victim");
        assertThat(captor.getValue().getEventClassName())
                .isEqualTo(CourseRatingRecalculatedIntegrationEvent.class.getName());
    }

    @Test
    void retryable_nonRetryableDataIntegrityViolation_neverRetries() {
        // given - DataIntegrityViolationException is NOT in retryFor list
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440005");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 5.0);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new DataIntegrityViolationException("rating constraint violation");
        }).when(commandHandler).handle(any());

        // when / then - non-retryable wraps in ExhaustedRetryException
        assertThatThrownBy(() -> handler.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_deadlockLoserWithNullMessage_recoversUsingClassName() {
        // given - DeadlockLoserDataAccessException with null message
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440006");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 1.0);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new DeadlockLoserDataAccessException(null, null);
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseRatingRecalculatedEvent(event);

        // then - null message falls back to class name
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage())
                .isEqualTo(DeadlockLoserDataAccessException.class.getName());
    }

    @Test
    void retryable_retryableOnFirstTwoThenDataIntegrityOnThird_propagatesWithoutRecover() {
        // given - retryable exceptions followed by a different non-retryable DataAccessException
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440007");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.0);
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) throw new PessimisticLockingFailureException("lock on first");
            if (count == 2) throw new OptimisticLockingFailureException("lock on second");
            throw new DataIntegrityViolationException("constraint on third");
        }).when(commandHandler).handle(any());

        // when / then - DataIntegrityViolationException is not retryable, no matching @Recover
        assertThatThrownBy(() -> handler.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);

        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_withBoundaryRatingValue_recoversWithCorrectPayload() {
        // given - edge case double value preserved through retry+recover flow
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440008");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, Double.MAX_VALUE);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("timeout with boundary value");
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseRatingRecalculatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventPayload()).contains(String.valueOf(Double.MAX_VALUE));
        assertThat(captor.getValue().getRetryCount()).isEqualTo(3);
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
