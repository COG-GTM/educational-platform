package com.educational.platform.config.retry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying Spring Retry behavior when different retryable exception
 * subtypes are thrown on different retry attempts.
 * <p>
 * Production handlers specify {@code @Retryable(retryFor = {DataAccessException.class,
 * ObjectOptimisticLockingFailureException.class})}. Since OOLFE is a DataAccessException
 * subclass, the recover method typed {@code recover(DataAccessException e, ...)} handles
 * both. This test verifies:
 * <ul>
 *   <li>Retry continues across different DataAccessException subtypes</li>
 *   <li>Recovery receives the LAST exception thrown (not the first)</li>
 *   <li>The attempt count is correct regardless of exception type variation</li>
 * </ul>
 */
@SpringJUnitConfig(MixedRetryableExceptionTypesIntegrationTest.TestConfig.class)
class MixedRetryableExceptionTypesIntegrationTest {

    @Autowired
    private MixedExceptionHandler handler;

    @Test
    void mixedDataAccessExceptions_allRetried_recoverReceivesLastException() {
        // given — each attempt throws a different DataAccessException subclass
        handler.reset();
        handler.setExceptionSequence(List.of(
                new DataAccessResourceFailureException("attempt 1: connection lost"),
                new ObjectOptimisticLockingFailureException("attempt 2: lock conflict", (Throwable) null),
                new QueryTimeoutException("attempt 3: timeout")
        ));

        // when
        handler.handleEvent("mixed-event-1");

        // then — all 3 attempts made, recover called with the last exception
        assertThat(handler.getAttemptCount()).isEqualTo(3);
        assertThat(handler.getRecoveredException())
                .isInstanceOf(QueryTimeoutException.class)
                .hasMessage("attempt 3: timeout");
        assertThat(handler.getRecoveredEventPayload()).isEqualTo("mixed-event-1");
    }

    @Test
    void dataAccessThenOptimisticLock_recoverReceivesOptimisticLockException() {
        // given — first attempt DB failure, second attempt optimistic lock
        handler.reset();
        handler.setExceptionSequence(List.of(
                new DataAccessResourceFailureException("DB down"),
                new ObjectOptimisticLockingFailureException("version mismatch", (Throwable) null),
                new ObjectOptimisticLockingFailureException("still conflicting", (Throwable) null)
        ));

        // when
        handler.handleEvent("mixed-event-2");

        // then — recover receives the last OOLFE
        assertThat(handler.getAttemptCount()).isEqualTo(3);
        assertThat(handler.getRecoveredException())
                .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                .hasMessage("still conflicting");
    }

    @Test
    void optimisticLockThenDataAccess_recoverReceivesDataAccessException() {
        // given — first attempt optimistic lock, subsequent attempts other DataAccess failures
        handler.reset();
        handler.setExceptionSequence(List.of(
                new ObjectOptimisticLockingFailureException("lock conflict", (Throwable) null),
                new DataIntegrityViolationException("constraint violation"),
                new DataAccessResourceFailureException("final connection failure")
        ));

        // when
        handler.handleEvent("mixed-event-3");

        // then — recover receives the last DataAccessResourceFailureException
        assertThat(handler.getAttemptCount()).isEqualTo(3);
        assertThat(handler.getRecoveredException())
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessage("final connection failure");
    }

    @Test
    void mixedExceptions_recoverReceivesExactLastInstance() {
        // given — verify same-instance identity (not just type equality)
        handler.reset();
        DataIntegrityViolationException lastException = new DataIntegrityViolationException("unique");
        handler.setExceptionSequence(List.of(
                new DataAccessResourceFailureException("first"),
                new ObjectOptimisticLockingFailureException("second", (Throwable) null),
                lastException
        ));

        // when
        handler.handleEvent("identity-check");

        // then — exact same instance
        assertThat(handler.getRecoveredException()).isSameAs(lastException);
    }

    @Test
    void mixedExceptions_successOnSecondAttempt_noRecover() {
        // given — first attempt fails, second succeeds
        handler.reset();
        handler.setExceptionSequence(List.of(
                new ObjectOptimisticLockingFailureException("transient lock", (Throwable) null)
        ));
        handler.setSucceedOnAttempt(2);

        // when
        handler.handleEvent("partial-success");

        // then — no recovery, only 2 attempts
        assertThat(handler.getAttemptCount()).isEqualTo(2);
        assertThat(handler.getRecoveredException()).isNull();
        assertThat(handler.getRecoveredEventPayload()).isNull();
    }

    @Test
    void allSameExceptionType_verifiesBaselineBehavior() {
        // given — same type on all attempts (baseline for comparison with mixed tests)
        handler.reset();
        handler.setExceptionSequence(List.of(
                new DataAccessResourceFailureException("same-1"),
                new DataAccessResourceFailureException("same-2"),
                new DataAccessResourceFailureException("same-3")
        ));

        // when
        handler.handleEvent("baseline");

        // then
        assertThat(handler.getAttemptCount()).isEqualTo(3);
        assertThat(handler.getRecoveredException())
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessage("same-3");
    }

    @Configuration
    @EnableRetry(proxyTargetClass = true)
    static class TestConfig {

        @Bean
        MixedExceptionHandler mixedExceptionHandler() {
            return new MixedExceptionHandler();
        }
    }

    /**
     * Test handler that throws a configurable sequence of exceptions across retry attempts.
     * Mirrors the production pattern: @Retryable for DataAccessException + OOLFE,
     * @Recover(DataAccessException) catches all subtypes.
     */
    static class MixedExceptionHandler {

        private final AtomicInteger attemptCount = new AtomicInteger(0);
        private final AtomicReference<DataAccessException> recoveredException = new AtomicReference<>();
        private final AtomicReference<String> recoveredEventPayload = new AtomicReference<>();
        private volatile List<DataAccessException> exceptionSequence = new ArrayList<>();
        private volatile int succeedOnAttempt = Integer.MAX_VALUE;

        void reset() {
            attemptCount.set(0);
            recoveredException.set(null);
            recoveredEventPayload.set(null);
            exceptionSequence = new ArrayList<>();
            succeedOnAttempt = Integer.MAX_VALUE;
        }

        @Retryable(retryFor = {DataAccessException.class, ObjectOptimisticLockingFailureException.class},
                   maxAttempts = 3, backoff = @Backoff(delay = 10, multiplier = 1))
        public void handleEvent(String eventPayload) {
            int attempt = attemptCount.incrementAndGet();
            if (attempt >= succeedOnAttempt) {
                return; // success
            }
            int index = Math.min(attempt - 1, exceptionSequence.size() - 1);
            throw exceptionSequence.get(index);
        }

        @Recover
        public void recover(DataAccessException e, String eventPayload) {
            recoveredException.set(e);
            recoveredEventPayload.set(eventPayload);
        }

        int getAttemptCount() { return attemptCount.get(); }
        DataAccessException getRecoveredException() { return recoveredException.get(); }
        String getRecoveredEventPayload() { return recoveredEventPayload.get(); }

        void setExceptionSequence(List<DataAccessException> sequence) {
            this.exceptionSequence = new ArrayList<>(sequence);
        }

        void setSucceedOnAttempt(int attempt) { this.succeedOnAttempt = attempt; }
    }
}
