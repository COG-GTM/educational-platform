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
import org.springframework.dao.CannotAcquireLockException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Spring Retry integration test for {@link StudentEnrolledToCourseIntegrationEventHandler}.
 * Validates the retry mechanism with an event containing UUID courseId and String username.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StudentEnrolledRetryIntegrationTest.RetryTestConfig.class)
class StudentEnrolledRetryIntegrationTest {

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
    void retryable_optimisticLockingFailure_retriesAndRecovers() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new OptimisticLockingFailureException("version mismatch");
        }).when(commandHandler).handle(any());

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("version mismatch");
        assertThat(captor.getValue().getEventPayload()).contains("student1");
    }

    @Test
    void retryable_succeedsOnFirstAttempt_noRetryAndNoRecovery() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student2");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            return null;
        }).when(commandHandler).handle(any());

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_nonRetryableNullPointerException_propagatesWithoutRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student3");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new NullPointerException("course not found");
        }).when(commandHandler).handle(any());

        // when / then - non-retryable wraps in ExhaustedRetryException
        assertThatThrownBy(() -> handler.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(NullPointerException.class);
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_pessimisticLockOnFirstAttemptThenSuccess_noRecovery() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440004");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student4");
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) {
                throw new PessimisticLockingFailureException("table locked");
            }
            return null;
        }).when(commandHandler).handle(any());

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(2);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void recover_eventPayloadContainsCourseIdAndUsername() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440005");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "john.doe");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("query exceeded 30s");
        }).when(commandHandler).handle(any());

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        FailedIntegrationEventRecord record = captor.getValue();
        assertThat(record.getEventPayload()).contains(uuid.toString());
        assertThat(record.getEventPayload()).contains("john.doe");
        assertThat(record.getEventClassName()).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class.getName());
        assertThat(record.getRetryCount()).isEqualTo(3);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void retryable_queryTimeout_retriesAndRecovers() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440006");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student6");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("query exceeded 30s");
        }).when(commandHandler).handle(any());

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void retryable_succeedsOnThirdAttempt_noRecovery() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440007");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student7");
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count < 3) {
                throw new OptimisticLockingFailureException("retry " + count);
            }
            return null;
        }).when(commandHandler).handle(any());

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_mixedExceptions_retriesUntilExhausted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440008");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student8");
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) throw new OptimisticLockingFailureException("attempt 1");
            if (count == 2) throw new PessimisticLockingFailureException("attempt 2");
            throw new QueryTimeoutException("attempt 3");
        }).when(commandHandler).handle(any());

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("attempt 3");
    }

    @Test
    void recover_withNullExceptionMessage_usesClassName() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440009");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student9");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new PessimisticLockingFailureException(null);
        }).when(commandHandler).handle(any());

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage())
                .isEqualTo(PessimisticLockingFailureException.class.getName());
    }

    @Test
    void retryable_cannotAcquireLockException_retriesAndRecovers() {
        // given - CannotAcquireLockException extends PessimisticLockingFailureException
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-42665544000b");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student-lock");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new CannotAcquireLockException("lock wait timeout exceeded");
        }).when(commandHandler).handle(any());

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("lock wait timeout exceeded");
        assertThat(captor.getValue().getEventPayload()).contains("student-lock");
    }

    @Test
    void recover_persistsCorrectFieldsAfterExhaustedRetries() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-42665544000a");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "jane.doe");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new OptimisticLockingFailureException("specific lock message");
        }).when(commandHandler).handle(any());

        // when
        handler.handleStudentEnrolledToCourseEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        FailedIntegrationEventRecord record = captor.getValue();
        assertThat(record.getEventClassName()).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class.getName());
        assertThat(record.getEventPayload()).contains(uuid.toString());
        assertThat(record.getEventPayload()).contains("jane.doe");
        assertThat(record.getExceptionMessage()).isEqualTo("specific lock message");
        assertThat(record.getRetryCount()).isEqualTo(3);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
        assertThat(record.getTimestamp()).isNotNull();
        assertThat(record.getId()).isNull();
    }

    @Test
    void retryable_recoverMethodThrowsException_propagatesOutOfRetryFramework() {
        // given - when @Recover itself throws (e.g., repository is down), exception escapes Spring Retry
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-42665544000c");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("transient failure");
        }).when(commandHandler).handle(any());
        doThrow(new RuntimeException("repository unavailable"))
                .when(failedEventRepository).save(any(FailedIntegrationEventRecord.class));

        // when / then - after exhausting retries, recover is called but throws, propagating out
        assertThatThrownBy(() -> handler.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("repository unavailable");

        assertThat(invocationCounter.get()).isEqualTo(3);
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
