package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
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
}
