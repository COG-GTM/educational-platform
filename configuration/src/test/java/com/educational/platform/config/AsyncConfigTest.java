package com.educational.platform.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTest {

    private final AsyncConfig sut = new AsyncConfig();

    @Test
    void integrationEventExecutor_isBoundedWithCallerRunsPolicy() {
        final ThreadPoolTaskExecutor executor = sut.integrationEventExecutor();
        // Spring initializes the executor through the bean lifecycle; do it manually here.
        executor.initialize();

        assertEquals(4, executor.getCorePoolSize());
        assertEquals(16, executor.getMaxPoolSize());
        assertEquals(500, executor.getQueueCapacity());
        assertEquals("integration-event-", executor.getThreadNamePrefix());
        assertTrue(executor.getThreadPoolExecutor().getRejectedExecutionHandler()
                instanceof ThreadPoolExecutor.CallerRunsPolicy);
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_returnsIntegrationEventExecutor() {
        final Executor asyncExecutor = sut.getAsyncExecutor();

        assertNotNull(asyncExecutor);
        assertInstanceOf(ThreadPoolTaskExecutor.class, asyncExecutor);
    }

    @Test
    void getAsyncUncaughtExceptionHandler_isProvided() {
        final AsyncUncaughtExceptionHandler handler = sut.getAsyncUncaughtExceptionHandler();

        assertNotNull(handler);
        // must not throw when logging an unhandled exception
        handler.handleUncaughtException(new IllegalStateException("boom"),
                AsyncConfigTest.class.getDeclaredMethods()[0]);
    }

    @Test
    void getAsyncUncaughtExceptionHandler_toleratesNullMessageAndParams() {
        final AsyncUncaughtExceptionHandler handler = sut.getAsyncUncaughtExceptionHandler();

        handler.handleUncaughtException(new IllegalStateException((String) null),
                AsyncConfigTest.class.getDeclaredMethods()[0], (Object) null);
    }

    @Test
    void integrationEventExecutor_runsTasksOnPrefixedThread() throws Exception {
        final ThreadPoolTaskExecutor executor = sut.integrationEventExecutor();
        executor.initialize();
        try {
            final CompletableFuture<String> threadName = new CompletableFuture<>();

            executor.execute(() -> threadName.complete(Thread.currentThread().getName()));

            assertTrue(threadName.get(5, TimeUnit.SECONDS).startsWith("integration-event-"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void asyncConfig_isSpringConfigurationWithRetryEnabled() {
        assertNotNull(AsyncConfig.class.getAnnotation(Configuration.class));
        assertNotNull(AsyncConfig.class.getAnnotation(EnableRetry.class));
    }

    @Test
    void getAsyncUncaughtExceptionHandler_logsErrorWithMethodAndException() {
        final ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(AsyncConfig.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            final AsyncUncaughtExceptionHandler handler = sut.getAsyncUncaughtExceptionHandler();
            final IllegalStateException failure = new IllegalStateException("boom");

            handler.handleUncaughtException(failure,
                    AsyncConfigTest.class.getDeclaredMethods()[0], "param");

            assertEquals(1, appender.list.size());
            final ILoggingEvent loggingEvent = appender.list.get(0);
            assertEquals(Level.ERROR, loggingEvent.getLevel());
            assertTrue(loggingEvent.getFormattedMessage()
                    .contains(AsyncConfigTest.class.getDeclaredMethods()[0].getName()));
            assertEquals("boom", loggingEvent.getThrowableProxy().getMessage());
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void saturatedExecutor_appliesBackPressureViaCallerRuns() throws Exception {
        final ThreadPoolTaskExecutor executor = sut.integrationEventExecutor();
        executor.initialize();
        try {
            final CountDownLatch release = new CountDownLatch(1);
            final CountDownLatch coreStarted = new CountDownLatch(4);
            final Runnable blockingTask = () -> {
                coreStarted.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };

            // occupy all core threads
            for (int i = 0; i < 4; i++) {
                executor.execute(blockingTask);
            }
            assertTrue(coreStarted.await(5, TimeUnit.SECONDS));
            // fill the queue
            for (int i = 0; i < 500; i++) {
                executor.execute(blockingTask);
            }
            // grow to max pool size
            for (int i = 0; i < 12; i++) {
                executor.execute(blockingTask);
            }

            // next task is rejected by the pool and runs on the caller thread
            final CompletableFuture<String> overflowThreadName = new CompletableFuture<>();
            executor.execute(() -> overflowThreadName.complete(Thread.currentThread().getName()));

            assertEquals(Thread.currentThread().getName(), overflowThreadName.get(5, TimeUnit.SECONDS));
            release.countDown();
        } finally {
            executor.shutdown();
        }
    }
}
