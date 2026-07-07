package com.educational.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class AsyncConfigTest {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    void integrationEventExecutor_isBoundedAndNamed() {
        ThreadPoolTaskExecutor executor = config.integrationEventExecutor();
        executor.initialize();

        assertEquals(AsyncConfig.CORE_POOL_SIZE, executor.getCorePoolSize());
        assertEquals(AsyncConfig.MAX_POOL_SIZE, executor.getMaxPoolSize());
        assertEquals(AsyncConfig.QUEUE_CAPACITY, executor.getQueueCapacity());
        assertTrue(executor.getThreadNamePrefix().startsWith(AsyncConfig.THREAD_NAME_PREFIX));
        assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class,
                executor.getThreadPoolExecutor().getRejectedExecutionHandler());

        executor.shutdown();
    }

    @Test
    void asyncExecutor_isProvided() {
        Executor executor = config.getAsyncExecutor();

        assertNotNull(executor);
        assertSame(config.integrationEventExecutor(), executor);
    }

    @Test
    void uncaughtExceptionHandler_isProvided() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();

        assertNotNull(handler);
        handler.handleUncaughtException(new RuntimeException("test"),
                AsyncConfig.class.getMethod("getAsyncExecutor"), new Object[0]);
    }

}
