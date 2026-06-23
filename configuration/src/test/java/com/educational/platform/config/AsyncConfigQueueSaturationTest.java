package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the {@link AsyncConfig} executor's behavior under queue saturation.
 * <p>
 * The executor is configured with:
 * <ul>
 *   <li>corePoolSize = 4</li>
 *   <li>maxPoolSize = 8</li>
 *   <li>queueCapacity = 100</li>
 *   <li>rejectedExecutionHandler = AbortPolicy</li>
 * </ul>
 * <p>
 * When all 8 threads are busy AND the queue is full (100 tasks queued), the next
 * submitted task must be rejected with {@link RejectedExecutionException}. This
 * behavior ensures backpressure propagates to callers rather than silently dropping
 * integration events.
 */
class AsyncConfigQueueSaturationTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void executor_rejectsTask_whenAllThreadsBusyAndQueueFull() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

        int maxPoolSize = executor.getMaxPoolSize();
        int queueCapacity = executor.getQueueCapacity();
        CountDownLatch blockLatch = new CountDownLatch(1);
        AtomicInteger startedCount = new AtomicInteger(0);

        try {
            // Fill all threads and queue to capacity
            int totalToSaturate = maxPoolSize + queueCapacity;
            for (int i = 0; i < totalToSaturate; i++) {
                executor.submit(() -> {
                    startedCount.incrementAndGet();
                    try { blockLatch.await(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
                });
            }

            // Wait for pool threads to start
            Thread.sleep(200);

            // Next submission must be rejected
            assertThatThrownBy(() -> executor.submit(() -> {}))
                    .isInstanceOf(RejectedExecutionException.class);
        } finally {
            blockLatch.countDown();
            executor.shutdown();
        }
    }

    @Test
    void executor_doesNotRejectTask_whenQueueHasCapacity() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

        CountDownLatch blockLatch = new CountDownLatch(1);
        CountDownLatch submitted = new CountDownLatch(1);

        try {
            // Submit one blocking task
            executor.submit(() -> {
                try { blockLatch.await(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
            });

            // Should succeed since queue is not full
            executor.submit(submitted::countDown);

            blockLatch.countDown();
            assertThat(submitted.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            blockLatch.countDown();
            executor.shutdown();
        }
    }

    @Test
    void executor_scalesUpToMaxPoolSize_underLoad() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

        int maxPoolSize = executor.getMaxPoolSize();
        int queueCapacity = executor.getQueueCapacity();
        CountDownLatch blockLatch = new CountDownLatch(1);
        CountDownLatch allStarted = new CountDownLatch(maxPoolSize);

        try {
            // Submit enough tasks to fill the queue AND force max threads
            int totalTasks = maxPoolSize + queueCapacity;
            for (int i = 0; i < totalTasks; i++) {
                executor.submit(() -> {
                    allStarted.countDown();
                    try { blockLatch.await(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
                });
            }

            // Wait for all max pool threads to start (they pick tasks from the queue)
            assertThat(allStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(executor.getActiveCount()).isGreaterThanOrEqualTo(maxPoolSize);
        } finally {
            blockLatch.countDown();
            executor.shutdown();
        }
    }
}
