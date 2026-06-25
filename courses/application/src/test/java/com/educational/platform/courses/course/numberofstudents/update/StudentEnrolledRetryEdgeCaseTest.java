package com.educational.platform.courses.course.numberofstudents.update;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * Additional Spring Retry edge-case integration tests for {@link StudentEnrolledToCourseIntegrationEventHandler}.
 * Covers exception transition scenarios (retryable → non-retryable on subsequent attempts)
 * and deep exception hierarchy subclasses.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StudentEnrolledRetryEdgeCaseTest.RetryTestConfig.class)
class StudentEnrolledRetryEdgeCaseTest {

    @Autowired
    private StudentEnrolledToCourseIntegrationEventHandler handler;

    @Autowired
    private IncreaseNumberOfStudentsCommandHandler commandHandler;

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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) {
                throw new QueryTimeoutException("transient on first attempt");
            }
            throw new IllegalStateException("invalid state on second attempt");
        }).when(commandHandler).handle(any());

        // when / then - non-retryable exception exhausts retry; @Recover only matches
        // TransientDataAccessException so ExhaustedRetryException wraps the original
        assertThatThrownBy(() -> handler.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(IllegalStateException.class);

        assertThat(invocationCounter.get()).isEqualTo(2);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_retryableExceptionsThenNonRetryableOnLastAttempt_propagatesWithoutRecover() {
        // given - two retryable exceptions, then non-retryable on final attempt
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student2");
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count <= 2) {
                throw new PessimisticLockingFailureException("retryable attempt " + count);
            }
            throw new NullPointerException("null on final attempt");
        }).when(commandHandler).handle(any());

        // when / then - all 3 attempts used; last throws non-retryable, so @Recover is NOT invoked;
        // ExhaustedRetryException wraps the non-retryable exception
        assertThatThrownBy(() -> handler.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(NullPointerException.class);

        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_nonRetryableDataIntegrityViolation_neverRetries() {
        // given - DataIntegrityViolationException is NOT in retryFor list
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student3");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new DataIntegrityViolationException("duplicate enrollment");
        }).when(commandHandler).handle(any());

        // when / then - non-retryable wraps in ExhaustedRetryException
        assertThatThrownBy(() -> handler.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_deadlockLoserException_retriesAndRecovers() {
        // given - DeadlockLoserDataAccessException extends PessimisticLockingFailureException
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440004");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student4");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new DeadlockLoserDataAccessException("deadlock victim", null);
        }).when(commandHandler).handle(any());

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("deadlock victim");
        assertThat(captor.getValue().getEventClassName())
                .isEqualTo(StudentEnrolledToCourseIntegrationEvent.class.getName());
        assertThat(captor.getValue().getEventPayload()).contains("student4");
    }

    @Test
    void retryable_deadlockLoserWithNullMessage_recoversUsingClassName() {
        // given - null message fallback to class name
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440005");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student5");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new DeadlockLoserDataAccessException(null, null);
        }).when(commandHandler).handle(any());

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage())
                .isEqualTo(DeadlockLoserDataAccessException.class.getName());
    }

    @Test
    void retryable_retryableOnFirstTwoThenDataIntegrityOnThird_propagatesWithoutRecover() {
        // given - retryable exceptions followed by non-retryable DataAccessException
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440006");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student6");
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) throw new OptimisticLockingFailureException("lock on first");
            if (count == 2) throw new PessimisticLockingFailureException("lock on second");
            throw new DataIntegrityViolationException("constraint on third");
        }).when(commandHandler).handle(any());

        // when / then - DataIntegrityViolationException is not retryable and has no matching
        // @Recover, so Spring Retry wraps it in ExhaustedRetryException
        assertThatThrownBy(() -> handler.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);

        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository, never()).save(any());
    }

    @Configuration
    @EnableRetry
    static class RetryTestConfig {

        @Bean
        AtomicInteger invocationCounter() {
            return new AtomicInteger(0);
        }

        @Bean
        IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler() {
            return mock(IncreaseNumberOfStudentsCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedIntegrationEventRepository() {
            return mock(FailedIntegrationEventRepository.class);
        }

        @Bean
        StudentEnrolledToCourseIntegrationEventHandler studentEnrolledToCourseIntegrationEventHandler(
                IncreaseNumberOfStudentsCommandHandler commandHandler,
                FailedIntegrationEventRepository repository) {
            return new StudentEnrolledToCourseIntegrationEventHandler(commandHandler, repository);
        }
    }
}
