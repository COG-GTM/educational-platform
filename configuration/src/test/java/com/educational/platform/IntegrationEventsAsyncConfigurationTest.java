package com.educational.platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegrationEventsAsyncConfigurationTest {

    private final IntegrationEventsAsyncConfiguration sut = new IntegrationEventsAsyncConfiguration();

    @Test
    void integrationEventExecutor_isBoundedWithCallerRunsPolicyAndNamedThreads() {
        final ThreadPoolTaskExecutor executor = sut.integrationEventExecutor();
        executor.initialize();
        try {
            assertEquals(4, executor.getCorePoolSize());
            assertEquals(8, executor.getMaxPoolSize());
            assertEquals(100, executor.getQueueCapacity());
            assertEquals("integration-event-", executor.getThreadNamePrefix());
            assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class,
                    executor.getThreadPoolExecutor().getRejectedExecutionHandler());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void getAsyncExecutor_returnsIntegrationEventExecutorBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(IntegrationEventsAsyncConfiguration.class)) {
            final IntegrationEventsAsyncConfiguration configuration = context.getBean(IntegrationEventsAsyncConfiguration.class);
            assertSame(context.getBean(IntegrationEventsAsyncConfiguration.INTEGRATION_EVENT_EXECUTOR), configuration.getAsyncExecutor());
        }
    }

    @Test
    void integrationEventExecutor_saturated_appliesBackPressureByRunningTaskOnCallerThread() throws InterruptedException {
        final ThreadPoolTaskExecutor executor = sut.integrationEventExecutor();
        executor.initialize();
        final CountDownLatch release = new CountDownLatch(1);
        try {
            final int saturatingTasks = executor.getMaxPoolSize() + executor.getQueueCapacity();
            final CountDownLatch started = new CountDownLatch(executor.getCorePoolSize());
            for (int i = 0; i < saturatingTasks; i++) {
                executor.execute(() -> {
                    started.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            assertTrue(started.await(5, TimeUnit.SECONDS));

            final Thread[] executingThread = new Thread[1];
            executor.execute(() -> executingThread[0] = Thread.currentThread());

            assertSame(Thread.currentThread(), executingThread[0]);
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void getAsyncUncaughtExceptionHandler_logsWithoutThrowing() throws NoSuchMethodException {
        assertNotNull(sut.getAsyncUncaughtExceptionHandler());
        sut.getAsyncUncaughtExceptionHandler().handleUncaughtException(
                new RuntimeException("expected failure"),
                IntegrationEventsAsyncConfiguration.class.getMethod("integrationEventExecutor"),
                "event");
    }

    @Test
    void getAsyncUncaughtExceptionHandler_noArguments_logsWithoutThrowing() throws NoSuchMethodException {
        sut.getAsyncUncaughtExceptionHandler().handleUncaughtException(
                new RuntimeException("expected failure"),
                IntegrationEventsAsyncConfiguration.class.getMethod("integrationEventExecutor"));
    }

}
