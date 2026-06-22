package com.educational.platform.config.retry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
 * Integration test verifying the combined retry + recover behavior for both
 * DataAccessException and ObjectOptimisticLockingFailureException.
 * <p>
 * This mirrors the exact production handler pattern where a single @Recover(DataAccessException)
 * method handles both exception types because ObjectOptimisticLockingFailureException is a
 * DataAccessException subclass. Verifies Spring Retry correctly dispatches recovery.
 * <p>
 * Also verifies:
 * - Non-retryable exceptions propagate without retry or recovery
 * - Partial success (succeeds on Nth attempt) doesn't trigger recovery
 * - Recovery receives the correct exception instance and event payload
 */
@SpringJUnitConfig(AsyncRetryRecoverIntegrationTest.TestConfig.class)
class AsyncRetryRecoverIntegrationTest {

    @Autowired
    private IntegrationEventHandlerMirror handler;

    @Test
    void dataAccessException_retriesMaxAttempts_thenRecovers() {
        // given — mirrors DataAccessResourceFailureException from production handlers
        handler.reset();

        // when
        handler.handleEvent("event-da", new DataAccessResourceFailureException("DB down"));

        // then
        assertThat(handler.getAttemptCount()).isEqualTo(3);
        assertThat(handler.getRecoveredEvent()).isEqualTo("event-da");
        assertThat(handler.getRecoveredException())
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessage("DB down");
    }

    @Test
    void optimisticLockException_retriesMaxAttempts_thenRecovers() {
        // given — ObjectOptimisticLockingFailureException is a DataAccessException subclass
        handler.reset();

        // when
        handler.handleEvent("event-opl",
                new ObjectOptimisticLockingFailureException("lock conflict", new RuntimeException()));

        // then — @Recover(DataAccessException) catches it because of inheritance
        assertThat(handler.getAttemptCount()).isEqualTo(3);
        assertThat(handler.getRecoveredEvent()).isEqualTo("event-opl");
        assertThat(handler.getRecoveredException())
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void nonRetryableException_doesNotRetry_doesNotRecover_propagates() {
        // given — mirrors ResourceNotFoundException or IllegalStateException from handlers
        handler.reset();

        // when/then
        assertThatThrownBy(() -> handler.handleEvent("event-nr",
                new IllegalStateException("business error")))
                .satisfiesAnyOf(
                        ex -> assertThat(ex).isInstanceOf(IllegalStateException.class),
                        ex -> assertThat(ex).hasCauseInstanceOf(IllegalStateException.class)
                );
        assertThat(handler.getAttemptCount()).isEqualTo(1);
        assertThat(handler.getRecoveredEvent()).isNull();
    }

    @Test
    void successOnFirstAttempt_noRetryOrRecover() {
        // given
        handler.reset();

        // when
        handler.handleEvent("event-ok", null);

        // then
        assertThat(handler.getAttemptCount()).isEqualTo(1);
        assertThat(handler.getRecoveredEvent()).isNull();
    }

    @Test
    void successOnSecondAttempt_noRecover() {
        // given
        handler.reset();
        handler.setFailUntilAttempt(2);

        // when
        handler.handleEventWithTransientFailure("event-partial");

        // then
        assertThat(handler.getAttemptCount()).isEqualTo(2);
        assertThat(handler.getRecoveredEvent()).isNull();
    }

    @Test
    void optimisticLockException_recoverReceivesExactExceptionInstance() {
        // given
        handler.reset();
        ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("optimistic lock", new RuntimeException("cause"));

        // when
        handler.handleEvent("event-exact", exception);

        // then — Spring Retry passes the last thrown exception to @Recover
        assertThat(handler.getRecoveredException()).isSameAs(exception);
    }

    @Test
    void recoverMethod_invocationCount_matchesFailureCount() {
        // given
        handler.reset();

        // when — call twice, both should fail and recover
        handler.handleEvent("event-a", new DataAccessResourceFailureException("fail-a"));
        handler.handleEvent("event-b", new DataAccessResourceFailureException("fail-b"));

        // then
        assertThat(handler.getAllRecoveredEvents()).containsExactly("event-a", "event-b");
    }

    @Test
    void backoffDelays_areRespected_totalTimeGreaterThanMinimum() {
        // given — backoff: delay=50ms, multiplier=2 → ~50ms + ~100ms = ~150ms minimum
        handler.reset();
        long start = System.currentTimeMillis();

        // when
        handler.handleEvent("event-timing", new DataAccessResourceFailureException("timed"));

        // then
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed).as("Backoff should take at least 100ms (50 + 100)").isGreaterThanOrEqualTo(100);
        assertThat(handler.getAttemptCount()).isEqualTo(3);
    }

    @Test
    void recoverFailure_propagatesException() {
        // given — simulates failedIntegrationEventRepository.save() throwing
        handler.reset();
        handler.setRecoverShouldFail(true);

        // when/then
        assertThatThrownBy(() -> handler.handleEvent("event-rf",
                new DataAccessResourceFailureException("original")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Recovery persistence failed");
    }

    @Configuration
    @EnableRetry(proxyTargetClass = true)
    static class TestConfig {

        @Bean
        IntegrationEventHandlerMirror integrationEventHandlerMirror() {
            return new IntegrationEventHandlerMirror();
        }
    }

    /**
     * Mirrors the production integration event handler pattern:
     * @Retryable for DataAccessException + ObjectOptimisticLockingFailureException,
     * single @Recover(DataAccessException) that catches both.
     */
    static class IntegrationEventHandlerMirror {

        private final AtomicInteger attemptCount = new AtomicInteger(0);
        private final AtomicReference<String> recoveredEvent = new AtomicReference<>();
        private final List<String> allRecoveredEvents = Collections.synchronizedList(new ArrayList<>());
        private final AtomicReference<DataAccessException> recoveredException = new AtomicReference<>();
        private volatile int failUntilAttempt = Integer.MAX_VALUE;
        private volatile boolean recoverShouldFail = false;

        void reset() {
            attemptCount.set(0);
            recoveredEvent.set(null);
            allRecoveredEvents.clear();
            recoveredException.set(null);
            failUntilAttempt = Integer.MAX_VALUE;
            recoverShouldFail = false;
        }

        @Retryable(retryFor = {DataAccessException.class, ObjectOptimisticLockingFailureException.class},
                   maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
        public void handleEvent(String eventPayload, RuntimeException exceptionToThrow) {
            attemptCount.incrementAndGet();
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
        }

        @Retryable(retryFor = {DataAccessException.class, ObjectOptimisticLockingFailureException.class},
                   maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
        public void handleEventWithTransientFailure(String eventPayload) {
            int current = attemptCount.incrementAndGet();
            if (current < failUntilAttempt) {
                throw new DataAccessResourceFailureException("Transient failure #" + current);
            }
        }

        @Recover
        public void recover(DataAccessException e, String eventPayload, RuntimeException ignored) {
            if (recoverShouldFail) {
                throw new RuntimeException("Recovery persistence failed");
            }
            recoveredException.set(e);
            recoveredEvent.set(eventPayload);
            allRecoveredEvents.add(eventPayload);
        }

        @Recover
        public void recover(DataAccessException e, String eventPayload) {
            if (recoverShouldFail) {
                throw new RuntimeException("Recovery persistence failed");
            }
            recoveredException.set(e);
            recoveredEvent.set(eventPayload);
            allRecoveredEvents.add(eventPayload);
        }

        int getAttemptCount() { return attemptCount.get(); }
        String getRecoveredEvent() { return recoveredEvent.get(); }
        List<String> getAllRecoveredEvents() { return new ArrayList<>(allRecoveredEvents); }
        DataAccessException getRecoveredException() { return recoveredException.get(); }

        void setFailUntilAttempt(int attempt) { this.failUntilAttempt = attempt; }
        void setRecoverShouldFail(boolean shouldFail) { this.recoverShouldFail = shouldFail; }
    }
}
