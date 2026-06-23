package com.educational.platform.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests thread-level properties of the executor created by {@link AsyncConfig}.
 * <p>
 * Integration event processing requires non-daemon threads so the JVM does not
 * terminate while events are still being processed. Daemon threads are killed
 * during JVM shutdown without completing their work, which would silently drop
 * in-flight integration events.
 */
class AsyncConfigThreadPropertiesTest {

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
    void executorThreads_areNotDaemon() throws Exception {
        // given
        AtomicBoolean isDaemon = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(1);

        // when
        executor.submit(() -> {
            isDaemon.set(Thread.currentThread().isDaemon());
            latch.countDown();
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(isDaemon.get())
                .as("Integration event threads must NOT be daemon — daemon threads are killed during JVM shutdown")
                .isFalse();
    }

    @Test
    void executorThreads_haveCorrectNamePrefix() throws Exception {
        // given
        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // when
        executor.submit(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(threadName.get()).startsWith("integration-event-");
    }

    @Test
    void executorThreads_haveNormalPriority() throws Exception {
        // given
        AtomicReference<Integer> threadPriority = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // when
        executor.submit(() -> {
            threadPriority.set(Thread.currentThread().getPriority());
            latch.countDown();
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(threadPriority.get())
                .as("Integration event threads should have normal priority")
                .isEqualTo(Thread.NORM_PRIORITY);
    }

    @Test
    void multipleExecutorThreads_allHaveCorrectPrefix() throws Exception {
        // given
        int threadCount = 4;
        CountDownLatch allStarted = new CountDownLatch(threadCount);
        CountDownLatch release = new CountDownLatch(1);
        String[] threadNames = new String[threadCount];

        // when
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                threadNames[idx] = Thread.currentThread().getName();
                allStarted.countDown();
                try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
            });
        }
        assertThat(allStarted.await(5, TimeUnit.SECONDS)).isTrue();
        release.countDown();

        // then
        for (int i = 0; i < threadCount; i++) {
            assertThat(threadNames[i])
                    .as("Thread %d should have integration-event- prefix", i)
                    .startsWith("integration-event-");
        }
    }

    @Test
    void multipleExecutorThreads_allAreNonDaemon() throws Exception {
        // given
        int threadCount = 4;
        CountDownLatch allStarted = new CountDownLatch(threadCount);
        CountDownLatch release = new CountDownLatch(1);
        boolean[] daemonFlags = new boolean[threadCount];

        // when
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                daemonFlags[idx] = Thread.currentThread().isDaemon();
                allStarted.countDown();
                try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
            });
        }
        assertThat(allStarted.await(5, TimeUnit.SECONDS)).isTrue();
        release.countDown();

        // then
        for (int i = 0; i < threadCount; i++) {
            assertThat(daemonFlags[i])
                    .as("Thread %d must not be a daemon thread", i)
                    .isFalse();
        }
    }

    @Test
    void executorThread_belongsToMainThreadGroup() throws Exception {
        // given
        AtomicReference<ThreadGroup> threadGroup = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // when
        executor.submit(() -> {
            threadGroup.set(Thread.currentThread().getThreadGroup());
            latch.countDown();
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(threadGroup.get()).isNotNull();
    }
}
