package com.educational.platform.administration.course.create;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Spring Retry integration test for {@link SendCourseToApproveIntegrationEventHandler}.
 * Validates that @Retryable and @Recover annotations work correctly with Spring's proxy mechanism.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SendCourseToApproveRetryIntegrationTest.RetryTestConfig.class)
class SendCourseToApproveRetryIntegrationTest {

    @Autowired
    private SendCourseToApproveIntegrationEventHandler handler;

    @Autowired
    private CreateCourseProposalCommandHandler commandHandler;

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
    void retryable_queryTimeoutException_retriesThreeTimesAndRecovers() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("connection timeout");
        }).when(commandHandler).handle(any());

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("connection timeout");
        assertThat(captor.getValue().getEventClassName()).isEqualTo(SendCourseToApproveIntegrationEvent.class.getName());
        assertThat(captor.getValue().getRetryCount()).isEqualTo(3);
    }

    @Test
    void retryable_cannotAcquireLockException_retriesAndRecovers() {
        // CannotAcquireLockException extends PessimisticLockingFailureException
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new CannotAcquireLockException("lock wait timeout exceeded");
        }).when(commandHandler).handle(any());

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then - retried 3 times and recovered
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void retryable_recoversOnSecondAttempt_noDeadLetterPersisted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) {
                throw new PessimisticLockingFailureException("temporary deadlock");
            }
            return null;
        }).when(commandHandler).handle(any());

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(2);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_nonRetryableDataIntegrityViolation_neverRetries() {
        // DataIntegrityViolationException is NOT in retryFor list
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440004");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new org.springframework.dao.DataIntegrityViolationException("unique constraint");
        }).when(commandHandler).handle(any());

        // when / then - non-retryable wraps in ExhaustedRetryException
        assertThatThrownBy(() -> handler.handleSendCourseToApproveEvent(event))
                .isInstanceOf(ExhaustedRetryException.class)
                .hasCauseInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryable_mixedExceptions_retriesUntilExhausted() {
        // given - throws different retryable exception types on each attempt
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440005");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) throw new OptimisticLockingFailureException("attempt 1");
            if (count == 2) throw new PessimisticLockingFailureException("attempt 2");
            throw new QueryTimeoutException("attempt 3");
        }).when(commandHandler).handle(any());

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then - all 3 attempts exhausted despite different exception types
        assertThat(invocationCounter.get()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        // the last exception's message is passed to @Recover
        assertThat(captor.getValue().getExceptionMessage()).isEqualTo("attempt 3");
    }

    @Test
    void retryable_optimisticLockingFailure_retriesAndRecovers() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440006");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new OptimisticLockingFailureException("version mismatch");
        }).when(commandHandler).handle(any());

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void retryable_pessimisticLockingFailure_retriesAndRecovers() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440007");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new PessimisticLockingFailureException("table locked");
        }).when(commandHandler).handle(any());

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void retryable_succeedsOnFirstAttempt_noRetryNoRecovery() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440008");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            return null;
        }).when(commandHandler).handle(any());

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void recover_withNullExceptionMessage_usesClassName() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440009");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new PessimisticLockingFailureException(null);
        }).when(commandHandler).handle(any());

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionMessage())
                .isEqualTo(PessimisticLockingFailureException.class.getName());
    }

    @Test
    void retryable_succeedsOnThirdAttempt_noRecovery() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-42665544000a");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count < 3) {
                throw new QueryTimeoutException("retry " + count);
            }
            return null;
        }).when(commandHandler).handle(any());

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        assertThat(invocationCounter.get()).isEqualTo(3);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void recover_persistsCorrectFieldsAfterExhaustedRetries() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-42665544000b");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new OptimisticLockingFailureException("specific lock conflict");
        }).when(commandHandler).handle(any());

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        FailedIntegrationEventRecord record = captor.getValue();
        assertThat(record.getEventClassName()).isEqualTo(SendCourseToApproveIntegrationEvent.class.getName());
        assertThat(record.getEventPayload()).contains(uuid.toString());
        assertThat(record.getExceptionMessage()).isEqualTo("specific lock conflict");
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
        CreateCourseProposalCommandHandler createCourseProposalCommandHandler() {
            return mock(CreateCourseProposalCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedIntegrationEventRepository() {
            return mock(FailedIntegrationEventRepository.class);
        }

        @Bean
        SendCourseToApproveIntegrationEventHandler sendCourseToApproveIntegrationEventHandler(
                CreateCourseProposalCommandHandler commandHandler,
                FailedIntegrationEventRepository repository) {
            return new SendCourseToApproveIntegrationEventHandler(commandHandler, repository);
        }
    }
}
