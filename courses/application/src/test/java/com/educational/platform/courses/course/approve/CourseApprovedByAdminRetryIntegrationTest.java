package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Spring Retry integration test verifying the actual retry and recovery mechanism
 * for {@link CourseApprovedByAdminIntegrationEventHandler}.
 *
 * Unlike unit tests that verify annotations via reflection, this test creates a real
 * Spring context with @EnableRetry and confirms that:
 * - @Retryable actually retries the method the configured number of times
 * - @Recover is invoked after exhausting all retry attempts
 * - Non-retryable exceptions are not retried and propagate immediately
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CourseApprovedByAdminRetryIntegrationTest.RetryTestConfig.class)
class CourseApprovedByAdminRetryIntegrationTest {

    @Autowired
    private CourseApprovedByAdminIntegrationEventHandler handler;

    @Autowired
    private ApproveCourseCommandHandler commandHandler;

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
    void retryable_retriesThreeTimesOnTransientException_thenCallsRecover() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("connection timeout");
        }).when(commandHandler).handle(any());

        // when - Spring Retry intercepts the call and retries
        handler.handleCourseApprovedByAdminEvent(event);

        // then - method was retried 3 times (maxAttempts=3) before calling @Recover
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository, times(1)).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void retryable_retriesOnOptimisticLockingFailure_thenCallsRecover() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new OptimisticLockingFailureException("optimistic lock conflict");
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository, times(1)).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void retryable_nonRetryableException_propagatesImmediatelyWithoutRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new IllegalStateException("business logic error");
        }).when(commandHandler).handle(any());

        // when / then - non-retryable exception is not retried; wraps in ExhaustedRetryException
        assertThatThrownBy(() -> handler.handleCourseApprovedByAdminEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(IllegalStateException.class);

        // only invoked once (no retry for non-retryable exceptions)
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_succeedsOnSecondAttempt_doesNotCallRecover() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440004");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count < 2) {
                throw new QueryTimeoutException("transient failure");
            }
            return null;
        }).when(commandHandler).handle(any());

        // when - first attempt fails, second succeeds
        handler.handleCourseApprovedByAdminEvent(event);

        // then - method invoked twice: first fails, second succeeds; no recovery needed
        assertThat(invocationCounter.get()).isEqualTo(2);
        verify(failedEventRepository, never()).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void recover_persistsCorrectFieldsAfterExhaustedRetries() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440005");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("specific timeout message");
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then
        var captor = org.mockito.ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        FailedIntegrationEventRecord record = captor.getValue();
        assertThat(record.getEventClassName()).isEqualTo(CourseApprovedByAdminIntegrationEvent.class.getName());
        assertThat(record.getEventPayload()).contains(uuid.toString());
        assertThat(record.getExceptionMessage()).isEqualTo("specific timeout message");
        assertThat(record.getRetryCount()).isEqualTo(3);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
        assertThat(record.getTimestamp()).isNotNull();
    }

    @Test
    void retryable_pessimisticLockingFailure_retriesAndRecovers() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440006");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new PessimisticLockingFailureException("table locked");
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository, times(1)).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void retryable_succeedsOnFirstAttempt_noRetryNoRecovery() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440007");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            return null;
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void recover_withNullExceptionMessage_usesClassName() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440008");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException(null);
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then
        var captor = org.mockito.ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage())
                .isEqualTo(QueryTimeoutException.class.getName());
    }

    @Test
    void retryable_succeedsOnThirdAttempt_noRecovery() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440009");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count < 3) {
                throw new OptimisticLockingFailureException("retry " + count);
            }
            return null;
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_mixedExceptions_retriesUntilExhausted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-42665544000a");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) throw new OptimisticLockingFailureException("attempt 1");
            if (count == 2) throw new PessimisticLockingFailureException("attempt 2");
            throw new QueryTimeoutException("attempt 3");
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = org.mockito.ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("attempt 3");
    }

    @Test
    void retryable_cannotAcquireLockException_retriesAndRecovers() {
        // given - CannotAcquireLockException extends PessimisticLockingFailureException
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-42665544000b");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new CannotAcquireLockException("lock wait timeout exceeded");
        }).when(commandHandler).handle(any());

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Configuration
    @EnableRetry
    static class RetryTestConfig {

        @Bean
        AtomicInteger invocationCounter() {
            return new AtomicInteger(0);
        }

        @Bean
        ApproveCourseCommandHandler approveCourseCommandHandler() {
            return mock(ApproveCourseCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedIntegrationEventRepository() {
            return mock(FailedIntegrationEventRepository.class);
        }

        @Bean
        CourseApprovedByAdminIntegrationEventHandler courseApprovedByAdminIntegrationEventHandler(
                ApproveCourseCommandHandler commandHandler,
                FailedIntegrationEventRepository repository) {
            return new CourseApprovedByAdminIntegrationEventHandler(commandHandler, repository);
        }
    }
}
