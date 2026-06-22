package com.educational.platform.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the behavioral aspects of AsyncConfig's executor shutdown configuration.
 * <p>
 * AsyncConfig sets {@code waitForTasksToCompleteOnShutdown(true)} and
 * {@code awaitTerminationSeconds(30)}, meaning the executor should wait for
 * running tasks to complete before terminating. This is critical for integration
 * event handlers: if the application shuts down mid-processing, we need events
 * to finish handling (or finish retrying) rather than being silently dropped.
 * <p>
 * These tests verify the actual shutdown behavior, complementing the
 * {@link AsyncConfigTest} which verifies the configuration values are set.
 */
class AsyncConfigShutdownBehaviorTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        if (!executor.getThreadPoolExecutor().isTerminated()) {
            executor.getThreadPoolExecutor().shutdownNow();
        }
    }

    @Test
    void shutdown_waitsForRunningTaskToComplete() throws Exception {
        // given — a task that takes some time to complete
        AtomicBoolean taskCompleted = new AtomicBoolean(false);
        CountDownLatch taskStarted = new CountDownLatch(1);

        executor.submit(() -> {
            taskStarted.countDown();
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
                return;
            }
            taskCompleted.set(true);
        });

        // wait for task to start
        assertThat(taskStarted.await(5, TimeUnit.SECONDS)).isTrue();

        // when — shutdown the executor
        executor.shutdown();

        // then — task should have completed before shutdown returned
        assertThat(taskCompleted.get())
                .as("Executor with waitForTasksToCompleteOnShutdown=true should wait for running tasks")
                .isTrue();
    }

    @Test
    void shutdown_waitsForQueuedTasksToComplete() throws Exception {
        // given — fill core threads and queue an additional task
        CountDownLatch blockLatch = new CountDownLatch(1);
        AtomicBoolean queuedTaskCompleted = new AtomicBoolean(false);
        CountDownLatch coreThreadsStarted = new CountDownLatch(executor.getCorePoolSize());

        // Block all core threads
        for (int i = 0; i < executor.getCorePoolSize(); i++) {
            executor.submit(() -> {
                coreThreadsStarted.countDown();
                try {
                    blockLatch.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
            });
        }

        assertThat(coreThreadsStarted.await(5, TimeUnit.SECONDS)).isTrue();

        // Queue one more task
        executor.submit(() -> queuedTaskCompleted.set(true));

        // when — release blocked threads then shutdown
        blockLatch.countDown();
        executor.shutdown();

        // then — queued task should have run before shutdown completed
        assertThat(queuedTaskCompleted.get())
                .as("Executor should process queued tasks before shutdown completes")
                .isTrue();
    }

    @Test
    void shutdown_multipleRunningTasks_allComplete() throws Exception {
        // given — multiple concurrent tasks
        int taskCount = 3;
        AtomicInteger completedCount = new AtomicInteger(0);
        CountDownLatch allStarted = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                allStarted.countDown();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    return;
                }
                completedCount.incrementAndGet();
            });
        }

        assertThat(allStarted.await(5, TimeUnit.SECONDS)).isTrue();

        // when
        executor.shutdown();

        // then
        assertThat(completedCount.get())
                .as("All running tasks should complete before shutdown")
                .isEqualTo(taskCount);
    }

    @Test
    void shutdown_executorIsTerminatedAfterShutdown() {
        // when
        executor.shutdown();

        // then
        assertThat(executor.getThreadPoolExecutor().isTerminated()).isTrue();
        assertThat(executor.getThreadPoolExecutor().isShutdown()).isTrue();
    }

    @Test
    void shutdown_noRunningTasks_completesImmediately() {
        // given — no tasks submitted
        long start = System.currentTimeMillis();

        // when
        executor.shutdown();

        // then — should not wait the full 30 seconds
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed)
                .as("Shutdown without running tasks should complete quickly")
                .isLessThan(5000);
    }

    @Test
    void shutdown_taskSubmittedDuringShutdown_isRejected() throws Exception {
        // given — start a long-running task
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch blockLatch = new CountDownLatch(1);

        executor.submit(() -> {
            taskStarted.countDown();
            try {
                blockLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        });

        assertThat(taskStarted.await(5, TimeUnit.SECONDS)).isTrue();

        // when — initiate shutdown in background
        new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            blockLatch.countDown();
        }).start();

        executor.shutdown();

        // then — executor should reject new tasks after shutdown
        assertThat(executor.getThreadPoolExecutor().isShutdown()).isTrue();
    }

    @Test
    void executorFromConfig_coreThreadsStayAlive_forFastEventProcessing() throws Exception {
        // given — submit and complete a task
        CountDownLatch taskDone = new CountDownLatch(1);
        executor.submit(taskDone::countDown);
        assertThat(taskDone.await(5, TimeUnit.SECONDS)).isTrue();

        // then — pool should maintain core threads (not time them out)
        Thread.sleep(200);
        assertThat(executor.getThreadPoolExecutor().getPoolSize())
                .as("Core threads should remain alive for fast event processing")
                .isGreaterThan(0);

        executor.shutdown();
    }
}
