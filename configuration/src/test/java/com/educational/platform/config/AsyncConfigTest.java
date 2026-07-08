package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTest {

    private final AsyncConfig sut = new AsyncConfig();

    @Test
    void integrationEventExecutor_isBoundedWithCallerRunsPolicy() {
        final ThreadPoolTaskExecutor executor = sut.integrationEventExecutor();

        assertEquals(4, executor.getCorePoolSize());
        assertEquals(16, executor.getMaxPoolSize());
        assertEquals(500, executor.getQueueCapacity());
        assertEquals("integration-event-", executor.getThreadNamePrefix());
        assertTrue(executor.getThreadPoolExecutor().getRejectedExecutionHandler()
                instanceof ThreadPoolExecutor.CallerRunsPolicy);
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
}
