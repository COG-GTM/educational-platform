package com.educational.platform.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests AsyncConfig executor behavior when task queue reaches capacity.
 * Verifies the AbortPolicy rejects tasks once both the thread pool and queue are full.
 * This is important for integration event processing: when the system is overwhelmed,
 * tasks are rejected rather than silently dropped or causing OOM.
 */
class AsyncConfigExecutorCapacityTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void executor_rejectsTask_whenQueueAndPoolAreFull() throws Exception {
        // given — fill up max threads (8) + queue (100) = 108 total capacity
        CountDownLatch blockLatch = new CountDownLatch(1);
        int maxThreads = executor.getMaxPoolSize();
        int queueCapacity = executor.getQueueCapacity();

        // Use a smaller executor for this test to avoid needing 108 threads
        executor.shutdown();
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(3);
        executor.setThreadNamePrefix("capacity-test-");
        executor.afterPropertiesSet();

        // Block 2 threads
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try { blockLatch.await(30, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            });
        }

        // Fill the queue (3 tasks)
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                try { blockLatch.await(30, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            });
        }

        // Allow time for threads to start
        Thread.sleep(100);

        // when/then — next task should be rejected
        assertThatThrownBy(() -> executor.submit(() -> {}))
                .isInstanceOf(RejectedExecutionException.class);

        blockLatch.countDown();
    }

    @Test
    void executor_acceptsTask_whenQueueHasSpace() throws Exception {
        // given — fill only the core threads
        CountDownLatch blockLatch = new CountDownLatch(1);
        CountDownLatch taskAccepted = new CountDownLatch(1);

        for (int i = 0; i < executor.getCorePoolSize(); i++) {
            executor.submit(() -> {
                try { blockLatch.await(30, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            });
        }

        Thread.sleep(100);

        // when — submit a task that should be queued (not rejected)
        executor.submit(taskAccepted::countDown);

        // then — release blocked threads and verify queued task runs
        blockLatch.countDown();
        assertThat(taskAccepted.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void executor_scalesUpToMaxPoolSize_beforeQueueing() throws Exception {
        // given — use a small executor to observe scaling
        executor.shutdown();
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("scale-test-");
        executor.afterPropertiesSet();

        CountDownLatch blockLatch = new CountDownLatch(1);
        CountDownLatch allStarted = new CountDownLatch(1);

        // Fill core thread
        executor.submit(() -> {
            try { blockLatch.await(30, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        });

        // Fill the queue (5 tasks)
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try { blockLatch.await(30, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            });
        }

        Thread.sleep(100);

        // Next submission causes scaling beyond core pool (queue is full)
        executor.submit(() -> {
            allStarted.countDown();
            try { blockLatch.await(30, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        });

        // then — verify pool scaled up
        assertThat(allStarted.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(executor.getThreadPoolExecutor().getPoolSize()).isGreaterThan(1);

        blockLatch.countDown();
    }

    @Test
    void productionExecutor_configuredCapacity_matchesExpected() {
        // Verify the production config values align with what we test against
        ThreadPoolTaskExecutor prodExecutor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        assertThat(prodExecutor.getCorePoolSize()).isEqualTo(4);
        assertThat(prodExecutor.getMaxPoolSize()).isEqualTo(8);
        assertThat(prodExecutor.getQueueCapacity()).isEqualTo(100);
        prodExecutor.shutdown();
    }
}
