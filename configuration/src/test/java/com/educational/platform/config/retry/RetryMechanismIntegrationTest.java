package com.educational.platform.config.retry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test verifying the Spring Retry mechanism works correctly with AOP proxies.
 * This mirrors the retry configuration used by the integration event handlers:
 * - @Retryable retries for configured exception types
 * - @Recover is invoked after retries are exhausted
 * - Non-retryable exceptions propagate immediately without retry
 */
@SpringJUnitConfig(RetryMechanismIntegrationTest.TestConfig.class)
class RetryMechanismIntegrationTest {

    @Autowired
    private RetryableService retryableService;

    @Test
    void retryableMethod_retryableException_retriesMaxAttemptsThenRecovers() {
        // when
        retryableService.reset();
        retryableService.handleEvent("test-event-1", new RetryableTransientException("DB down"));

        // then
        assertThat(retryableService.getAttemptCount()).isEqualTo(3);
        assertThat(retryableService.getRecoveredEvents()).containsExactly("test-event-1");
    }

    @Test
    void retryableMethod_retryableSubclassException_retriesMaxAttemptsThenRecovers() {
        // when
        retryableService.reset();
        retryableService.handleEvent("test-event-2", new RetryableSubclassException("lock conflict"));

        // then
        assertThat(retryableService.getAttemptCount()).isEqualTo(3);
        assertThat(retryableService.getRecoveredEvents()).containsExactly("test-event-2");
    }

    @Test
    void retryableMethod_nonRetryableException_doesNotRetryAndDoesNotRecover() {
        // when/then — non-retryable exceptions are not retried; Spring Retry may
        // wrap them if no matching @Recover exists, but the key contract is:
        // 1) only 1 attempt (no retry), 2) @Recover is NOT invoked
        retryableService.reset();
        assertThatThrownBy(() -> retryableService.handleEvent("test-event-3",
                new NonRetryableBusinessException("business error")))
                .satisfiesAnyOf(
                        ex -> assertThat(ex).isInstanceOf(NonRetryableBusinessException.class),
                        ex -> assertThat(ex).hasCauseInstanceOf(NonRetryableBusinessException.class)
                );
        assertThat(retryableService.getAttemptCount()).isEqualTo(1);
        assertThat(retryableService.getRecoveredEvents()).isEmpty();
    }

    @Test
    void retryableMethod_succeedsOnFirstAttempt_noRetryOrRecover() {
        // when
        retryableService.reset();
        retryableService.handleEvent("test-event-4", null);

        // then
        assertThat(retryableService.getAttemptCount()).isEqualTo(1);
        assertThat(retryableService.getRecoveredEvents()).isEmpty();
    }

    @Test
    void retryableMethod_succeedsOnSecondAttempt_noRecover() {
        // given
        retryableService.reset();
        retryableService.setFailUntilAttempt(2);

        // when
        retryableService.handleEventWithTransientFailure("test-event-5");

        // then
        assertThat(retryableService.getAttemptCount()).isEqualTo(2);
        assertThat(retryableService.getRecoveredEvents()).isEmpty();
    }

    @Test
    void retryableMethod_succeedsOnThirdAttempt_noRecover() {
        // given
        retryableService.reset();
        retryableService.setFailUntilAttempt(3);

        // when
        retryableService.handleEventWithTransientFailure("test-event-6");

        // then
        assertThat(retryableService.getAttemptCount()).isEqualTo(3);
        assertThat(retryableService.getRecoveredEvents()).isEmpty();
    }

    @Test
    void retryableMethod_failsAllAttempts_recoverReceivesLastException() {
        // when
        retryableService.reset();
        retryableService.handleEvent("test-event-7",
                new RetryableTransientException("specific DB error"));

        // then
        assertThat(retryableService.getLastRecoveredException())
                .isInstanceOf(RetryableTransientException.class)
                .hasMessage("specific DB error");
    }

    @Test
    void recoverMethod_receivesCorrectEventPayload() {
        // when
        retryableService.reset();
        retryableService.handleEvent("my-unique-payload",
                new RetryableTransientException("DB error"));

        // then
        assertThat(retryableService.getRecoveredEvents()).containsExactly("my-unique-payload");
    }

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        RetryableService retryableService() {
            return new RetryableService();
        }
    }

    // Mirrors DataAccessException hierarchy used in handlers
    static class RetryableTransientException extends RuntimeException {
        RetryableTransientException(String message) { super(message); }
    }

    // Mirrors ObjectOptimisticLockingFailureException (subclass of retryable base)
    static class RetryableSubclassException extends RetryableTransientException {
        RetryableSubclassException(String message) { super(message); }
    }

    // Mirrors ResourceNotFoundException (non-retryable business exceptions)
    static class NonRetryableBusinessException extends RuntimeException {
        NonRetryableBusinessException(String message) { super(message); }
    }

    static class RetryableService {

        private final AtomicInteger attemptCount = new AtomicInteger(0);
        private final List<String> recoveredEvents = Collections.synchronizedList(new ArrayList<>());
        private final AtomicReference<RuntimeException> lastRecoveredException = new AtomicReference<>();
        private volatile int failUntilAttempt = Integer.MAX_VALUE;

        void reset() {
            attemptCount.set(0);
            recoveredEvents.clear();
            lastRecoveredException.set(null);
            failUntilAttempt = Integer.MAX_VALUE;
        }

        @Retryable(retryFor = {RetryableTransientException.class},
                   maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
        public void handleEvent(String eventPayload, RuntimeException exceptionToThrow) {
            attemptCount.incrementAndGet();
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
        }

        @Retryable(retryFor = {RetryableTransientException.class},
                   maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
        public void handleEventWithTransientFailure(String eventPayload) {
            int currentAttempt = attemptCount.incrementAndGet();
            if (currentAttempt < failUntilAttempt) {
                throw new RetryableTransientException("Transient failure on attempt " + currentAttempt);
            }
        }

        @Recover
        public void recover(RetryableTransientException e, String eventPayload, RuntimeException ignored) {
            lastRecoveredException.set(e);
            recoveredEvents.add(eventPayload);
        }

        @Recover
        public void recover(RetryableTransientException e, String eventPayload) {
            lastRecoveredException.set(e);
            recoveredEvents.add(eventPayload);
        }

        public int getAttemptCount() {
            return attemptCount.get();
        }

        public List<String> getRecoveredEvents() {
            return new ArrayList<>(recoveredEvents);
        }

        public RuntimeException getLastRecoveredException() {
            return lastRecoveredException.get();
        }

        public void setFailUntilAttempt(int attempt) {
            this.failUntilAttempt = attempt;
        }
    }
}
