package com.educational.platform.config.retry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that {@code @EnableAsync} and {@code @EnableRetry}
 * coexist in a single Spring context with the same ordering configuration as
 * the production {@code AsyncConfig} and {@code RetryConfig}.
 * <p>
 * Verifies:
 * <ul>
 *   <li>Both async and retry proxies activate without conflict</li>
 *   <li>@Async methods execute on the configured thread pool</li>
 *   <li>@Retryable methods within async context retry correctly</li>
 *   <li>@Recover is invoked after exhausted retries within async execution</li>
 * </ul>
 * <p>
 * The handler splits async dispatch and retryable logic into separate methods
 * to avoid proxy ordering ambiguity (matching the production internal pattern
 * where retry wraps the handler execution within the async thread).
 */
@SpringJUnitConfig(AsyncRetryRecoverEndToEndTest.TestConfig.class)
class AsyncRetryRecoverEndToEndTest {

    @Autowired
    private AsyncDispatcher asyncDispatcher;

    @Autowired
    private RetryableProcessor retryableProcessor;

    @Test
    void asyncDispatch_executesOnIntegrationEventThread() throws Exception {
        // given
        retryableProcessor.reset();

        // when — call through the async proxy
        asyncDispatcher.dispatch("async-event-1");

        // then — verify execution happened on the async thread pool
        String threadName = retryableProcessor.awaitThreadName(10, TimeUnit.SECONDS);
        assertThat(threadName).isNotNull().startsWith("integration-event-");
    }

    @Test
    void retryableMethod_retriesAndRecovers_withinAsyncContext() throws Exception {
        // given
        retryableProcessor.reset();
        retryableProcessor.setAlwaysFail(true);

        // when — async dispatch triggers retryable method
        asyncDispatcher.dispatch("retry-event-1");

        // then — recovery is invoked after max retries, on the async thread
        RecoveryResult result = retryableProcessor.awaitRecovery(10, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
        assertThat(result.eventPayload()).isEqualTo("retry-event-1");
        assertThat(result.attemptCount()).isEqualTo(3);
        assertThat(result.threadName()).startsWith("integration-event-");
    }

    @Test
    void retryableMethod_succeedsOnFirstAttempt_noRecovery() throws Exception {
        // given
        retryableProcessor.reset();
        retryableProcessor.setAlwaysFail(false);

        // when
        asyncDispatcher.dispatch("success-event-1");

        // then
        String threadName = retryableProcessor.awaitThreadName(10, TimeUnit.SECONDS);
        assertThat(threadName).startsWith("integration-event-");

        RecoveryResult recovery = retryableProcessor.pollRecovery(500, TimeUnit.MILLISECONDS);
        assertThat(recovery).isNull();
    }

    @Test
    void retryableMethod_succeedsOnSecondAttempt_noRecovery() throws Exception {
        // given
        retryableProcessor.reset();
        retryableProcessor.setFailUntilAttempt(2);

        // when
        asyncDispatcher.dispatch("partial-event-1");

        // then — success after 2nd attempt, no recovery
        String threadName = retryableProcessor.awaitThreadName(10, TimeUnit.SECONDS);
        assertThat(threadName).startsWith("integration-event-");
        assertThat(retryableProcessor.getAttemptCount()).isEqualTo(2);

        RecoveryResult recovery = retryableProcessor.pollRecovery(500, TimeUnit.MILLISECONDS);
        assertThat(recovery).isNull();
    }

    // --- Test infrastructure ---

    record RecoveryResult(String eventPayload, int attemptCount, String threadName,
                          String exceptionMessage) {}

    @Configuration
    @EnableAsync(order = Ordered.HIGHEST_PRECEDENCE, proxyTargetClass = true)
    @EnableRetry(order = Ordered.LOWEST_PRECEDENCE, proxyTargetClass = true)
    static class TestConfig {

        @Bean
        AsyncDispatcher asyncDispatcher(RetryableProcessor processor) {
            return new AsyncDispatcher(processor);
        }

        @Bean
        RetryableProcessor retryableProcessor() {
            return new RetryableProcessor();
        }

        @Bean(name = "integrationEventExecutor")
        Executor integrationEventExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(4);
            executor.setMaxPoolSize(8);
            executor.setQueueCapacity(100);
            executor.setThreadNamePrefix("integration-event-");
            executor.setWaitForTasksToCompleteOnShutdown(true);
            executor.setAwaitTerminationSeconds(30);
            executor.initialize();
            return executor;
        }
    }

    /**
     * Mirrors the @Async dispatch layer of production handlers.
     */
    static class AsyncDispatcher {

        private final RetryableProcessor processor;

        AsyncDispatcher(RetryableProcessor processor) {
            this.processor = processor;
        }

        @Async("integrationEventExecutor")
        public void dispatch(String eventPayload) {
            processor.process(eventPayload);
        }
    }

    /**
     * Mirrors the @Retryable + @Recover pattern of production handlers.
     */
    static class RetryableProcessor {

        private static final int MAX_ATTEMPTS = 3;
        private final AtomicInteger attemptCount = new AtomicInteger(0);
        private final BlockingQueue<RecoveryResult> recoveryResults = new LinkedBlockingQueue<>();
        private final BlockingQueue<String> threadNames = new LinkedBlockingQueue<>();
        private volatile boolean alwaysFail = false;
        private volatile int failUntilAttempt = 0;

        void reset() {
            attemptCount.set(0);
            recoveryResults.clear();
            threadNames.clear();
            alwaysFail = false;
            failUntilAttempt = 0;
        }

        void setAlwaysFail(boolean alwaysFail) {
            this.alwaysFail = alwaysFail;
        }

        void setFailUntilAttempt(int failUntilAttempt) {
            this.failUntilAttempt = failUntilAttempt;
        }

        int getAttemptCount() {
            return attemptCount.get();
        }

        @Retryable(retryFor = DataAccessResourceFailureException.class,
                   maxAttempts = MAX_ATTEMPTS,
                   backoff = @Backoff(delay = 50, multiplier = 2))
        public void process(String eventPayload) {
            int attempt = attemptCount.incrementAndGet();
            if (alwaysFail || attempt < failUntilAttempt) {
                throw new DataAccessResourceFailureException("Transient DB failure");
            }
            threadNames.offer(Thread.currentThread().getName());
        }

        @Recover
        public void recover(DataAccessResourceFailureException e, String eventPayload) {
            recoveryResults.offer(new RecoveryResult(
                    eventPayload,
                    attemptCount.get(),
                    Thread.currentThread().getName(),
                    e.getMessage()));
            threadNames.offer(Thread.currentThread().getName());
        }

        RecoveryResult awaitRecovery(long timeout, TimeUnit unit) throws InterruptedException {
            return recoveryResults.poll(timeout, unit);
        }

        RecoveryResult pollRecovery(long timeout, TimeUnit unit) throws InterruptedException {
            return recoveryResults.poll(timeout, unit);
        }

        String awaitThreadName(long timeout, TimeUnit unit) throws InterruptedException {
            return threadNames.poll(timeout, unit);
        }
    }
}
