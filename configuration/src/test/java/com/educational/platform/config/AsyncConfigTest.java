package com.educational.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LoggingAsyncUncaughtExceptionHandler.class, AsyncConfig.class);

    @Test
    void integrationEventExecutorBeanIsPresentAndConfigured() {
        contextRunner.run(context -> {
            assertThat(context).hasBean(AsyncConfig.INTEGRATION_EVENT_EXECUTOR);

            final ThreadPoolTaskExecutor executor =
                    context.getBean(AsyncConfig.INTEGRATION_EVENT_EXECUTOR, ThreadPoolTaskExecutor.class);

            assertThat(executor.getCorePoolSize()).isEqualTo(AsyncConfig.CORE_POOL_SIZE);
            assertThat(executor.getMaxPoolSize()).isEqualTo(AsyncConfig.MAX_POOL_SIZE);
            assertThat(executor.getQueueCapacity()).isEqualTo(AsyncConfig.QUEUE_CAPACITY);
            assertThat(executor.getThreadNamePrefix()).isEqualTo(AsyncConfig.THREAD_NAME_PREFIX);
        });
    }

    @Test
    void asyncConfigurerExposesExecutorAndUncaughtExceptionHandler() {
        contextRunner.run(context -> {
            final AsyncConfigurer asyncConfigurer = context.getBean(AsyncConfig.class);

            assertThat(asyncConfigurer.getAsyncExecutor())
                    .isSameAs(context.getBean(AsyncConfig.INTEGRATION_EVENT_EXECUTOR));
            assertThat(asyncConfigurer.getAsyncUncaughtExceptionHandler())
                    .isInstanceOf(LoggingAsyncUncaughtExceptionHandler.class);
        });
    }

    @Test
    void executorRunsTasksOnThreadsWithConfiguredPrefix() {
        contextRunner.run(context -> {
            final ThreadPoolTaskExecutor executor =
                    context.getBean(AsyncConfig.INTEGRATION_EVENT_EXECUTOR, ThreadPoolTaskExecutor.class);

            final CompletableFuture<String> threadName = new CompletableFuture<>();
            executor.execute(() -> threadName.complete(Thread.currentThread().getName()));

            assertThat(threadName.get(5, TimeUnit.SECONDS)).startsWith(AsyncConfig.THREAD_NAME_PREFIX);
        });
    }

    @Test
    void saturatedExecutorAppliesBackPressureByRunningOverflowTaskOnCallerThread() {
        contextRunner.run(context -> {
            final ThreadPoolTaskExecutor executor =
                    context.getBean(AsyncConfig.INTEGRATION_EVENT_EXECUTOR, ThreadPoolTaskExecutor.class);

            final CountDownLatch release = new CountDownLatch(1);
            final CountDownLatch allWorkersBusy = new CountDownLatch(AsyncConfig.MAX_POOL_SIZE);
            try {
                for (int i = 0; i < AsyncConfig.MAX_POOL_SIZE + AsyncConfig.QUEUE_CAPACITY; i++) {
                    executor.execute(() -> {
                        allWorkersBusy.countDown();
                        awaitQuietly(release);
                    });
                }
                assertThat(allWorkersBusy.await(5, TimeUnit.SECONDS)).isTrue();

                final CompletableFuture<Thread> overflowThread = new CompletableFuture<>();
                executor.execute(() -> overflowThread.complete(Thread.currentThread()));

                assertThat(overflowThread.getNow(null)).isSameAs(Thread.currentThread());
            } finally {
                release.countDown();
            }
        });
    }

    @Test
    void asyncConfigIsSpringConfiguration() {
        assertThat(AsyncConfig.class.isAnnotationPresent(Configuration.class)).isTrue();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
