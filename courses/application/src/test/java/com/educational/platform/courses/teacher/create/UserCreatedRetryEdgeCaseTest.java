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

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Additional Spring Retry edge-case integration tests for {@link UserCreatedIntegrationEventHandler}.
 * Covers exception transition scenarios and deep exception hierarchy subclasses
 * that complement the base {@link UserCreatedRetryIntegrationTest}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserCreatedRetryEdgeCaseTest.RetryTestConfig.class)
class UserCreatedRetryEdgeCaseTest {

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
    void retryable_retryableExceptionThenNonRetryable_stopsRetryAndPropagates() {
        // given - first attempt throws retryable, second throws non-retryable
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("user1", "user1@example.com");
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) {
                throw new QueryTimeoutException("transient on first attempt");
            }
            throw new IllegalArgumentException("business error on second attempt");
        }).when(commandHandler).handle(any());

        // when / then - Spring Retry retries after first (retryable) exception,
        // but second (non-retryable) exception exhausts retry; @Recover only matches
        // TransientDataAccessException so ExhaustedRetryException wraps the original
        assertThatThrownBy(() -> handler.handleUserCreatedEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);

        assertThat(invocationCounter.get()).isEqualTo(2);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_retryableExceptionsThenNonRetryableOnLastAttempt_propagatesWithoutRecover() {
        // given - two retryable exceptions, then non-retryable on final attempt
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("user2", "user2@example.com");
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count <= 2) {
                throw new OptimisticLockingFailureException("retryable attempt " + count);
            }
            throw new NullPointerException("unexpected null on final attempt");
        }).when(commandHandler).handle(any());

        // when / then - all 3 attempts used; last throws non-retryable, so @Recover is NOT invoked;
        // ExhaustedRetryException wraps the non-retryable exception
        assertThatThrownBy(() -> handler.handleUserCreatedEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(NullPointerException.class);

        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_cannotAcquireLockException_retriesAndRecovers() {
        // given - CannotAcquireLockException extends PessimisticLockingFailureException
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("user3", "user3@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new CannotAcquireLockException("lock wait timeout exceeded");
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("lock wait timeout exceeded");
    }

    @Test
    void retryable_deadlockLoserException_retriesAndRecovers() {
        // given - DeadlockLoserDataAccessException extends PessimisticLockingFailureException
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("user4", "user4@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new DeadlockLoserDataAccessException("deadlock victim", null);
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("deadlock victim");
        assertThat(captor.getValue().getEventClassName()).isEqualTo(UserCreatedIntegrationEvent.class.getName());
    }

    @Test
    void retryable_nonRetryableDataIntegrityViolation_neverRetries() {
        // given - DataIntegrityViolationException is NOT in retryFor list
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("user5", "user5@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new DataIntegrityViolationException("unique constraint violation on username");
        }).when(commandHandler).handle(any());

        // when / then - non-retryable wraps in ExhaustedRetryException
        assertThatThrownBy(() -> handler.handleUserCreatedEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_deadlockLoserWithNullMessage_recoversUsingClassName() {
        // given - DeadlockLoserDataAccessException with null message
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("user6", "user6@example.com");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new DeadlockLoserDataAccessException(null, null);
        }).when(commandHandler).handle(any());

        // when
        handler.handleUserCreatedEvent(event);

        // then - null message falls back to class name
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage())
                .isEqualTo(DeadlockLoserDataAccessException.class.getName());
    }

    @Test
    void retryable_retryableOnFirstTwoThenDataIntegrityOnThird_propagatesWithoutRecover() {
        // given - retryable exceptions followed by a different non-retryable DataAccessException
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("user7", "user7@example.com");
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) throw new PessimisticLockingFailureException("lock on first");
            if (count == 2) throw new OptimisticLockingFailureException("lock on second");
            throw new DataIntegrityViolationException("constraint on third");
        }).when(commandHandler).handle(any());

        // when / then - DataIntegrityViolationException is not retryable and has no matching
        // @Recover, so Spring Retry wraps it in ExhaustedRetryException
        assertThatThrownBy(() -> handler.handleUserCreatedEvent(event))
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
