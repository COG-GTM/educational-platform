package com.educational.platform.courses.teacher.create;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Spring Retry integration test for {@link UserCreatedIntegrationEventHandler}.
 * Validates that the @Retryable/@Recover annotations function correctly in a real Spring context.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserCreatedRetryIntegrationTest.RetryTestConfig.class)
class UserCreatedRetryIntegrationTest {

    @Autowired
    private UserCreatedIntegrationEventHandler handler;

    @Autowired
    private CreateTeacherCommandHandler commandHandler;

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
    void retryable_transientResourceException_retriesAndRecovers() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new TransientDataAccessResourceException("connection pool exhausted");
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("connection pool exhausted");
        assertThat(captor.getValue().getEventClassName()).isEqualTo(UserCreatedIntegrationEvent.class.getName());
    }

    @Test
    void retryable_pessimisticLockingException_retriesAndRecovers() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher2", "teacher2@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new PessimisticLockingFailureException("deadlock detected");
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void retryable_successOnThirdAttempt_noRecovery() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher3", "teacher3@example.com");
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count < 3) {
                throw new OptimisticLockingFailureException("retry " + count);
            }
            return null;
        }).when(commandHandler).handle(any());

        // when - succeeds on the 3rd attempt (the last chance before recovery)
        handler.handleUserCreatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_nonRetryableIllegalArgException_neverRetries() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher4", "teacher4@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new IllegalArgumentException("invalid username format");
        }).when(commandHandler).handle(any());

        // when / then - non-retryable wraps in ExhaustedRetryException
        assertThatThrownBy(() -> handler.handleUserCreatedEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void recover_withNullExceptionMessage_usesClassName() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher5", "teacher5@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new OptimisticLockingFailureException(null);
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage())
                .isEqualTo(OptimisticLockingFailureException.class.getName());
    }

    @Test
    void retryable_queryTimeout_retriesAndRecovers() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher6", "teacher6@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("query exceeded 30s");
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void retryable_succeedsOnFirstAttempt_noRetryNoRecovery() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher7", "teacher7@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            return null;
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_mixedExceptions_retriesUntilExhausted() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher8", "teacher8@example.com");
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) throw new OptimisticLockingFailureException("attempt 1");
            if (count == 2) throw new PessimisticLockingFailureException("attempt 2");
            throw new TransientDataAccessResourceException("attempt 3");
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("attempt 3");
    }

    @Test
    void recover_eventPayloadContainsUsernameAndEmail() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("john.doe", "john.doe@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("db timeout");
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        FailedIntegrationEventRecord record = captor.getValue();
        assertThat(record.getEventPayload()).contains("john.doe");
        assertThat(record.getEventPayload()).contains("john.doe@example.com");
        assertThat(record.getEventClassName()).isEqualTo(UserCreatedIntegrationEvent.class.getName());
        assertThat(record.getRetryCount()).isEqualTo(3);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
        assertThat(record.getTimestamp()).isNotNull();
    }

    @Test
    void retryable_optimisticLockingFailure_retriesAndRecovers() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher9", "teacher9@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new OptimisticLockingFailureException("version mismatch");
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void retryable_succeedsOnSecondAttempt_noRecovery() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher10", "teacher10@example.com");
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count < 2) {
                throw new QueryTimeoutException("transient failure");
            }
            return null;
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(2);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void recover_persistsCorrectFieldsAfterExhaustedRetries() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("john.smith", "john.smith@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new PessimisticLockingFailureException("specific deadlock message");
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        FailedIntegrationEventRecord record = captor.getValue();
        assertThat(record.getEventClassName()).isEqualTo(UserCreatedIntegrationEvent.class.getName());
        assertThat(record.getEventPayload()).contains("john.smith");
        assertThat(record.getEventPayload()).contains("john.smith@example.com");
        assertThat(record.getExceptionMessage()).isEqualTo("specific deadlock message");
        assertThat(record.getRetryCount()).isEqualTo(3);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
        assertThat(record.getTimestamp()).isNotNull();
        assertThat(record.getId()).isNull();
    }

    @Configuration
    @EnableRetry
    static class RetryTestConfig {

        @Bean
        AtomicInteger invocationCounter() {
            return new AtomicInteger(0);
        }

        @Bean
        CreateTeacherCommandHandler createTeacherCommandHandler() {
            return mock(CreateTeacherCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedIntegrationEventRepository() {
            return mock(FailedIntegrationEventRepository.class);
        }

        @Bean
        UserCreatedIntegrationEventHandler userCreatedIntegrationEventHandler(
                CreateTeacherCommandHandler commandHandler,
                FailedIntegrationEventRepository repository) {
            return new UserCreatedIntegrationEventHandler(commandHandler, repository);
        }
    }
}
