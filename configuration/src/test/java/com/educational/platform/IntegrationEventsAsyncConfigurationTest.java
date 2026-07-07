package com.educational.platform;

import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Test;
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
    void getAsyncExecutor_returnsIntegrationEventExecutor() {
        assertSame(sut.integrationEventExecutor().getClass(), sut.getAsyncExecutor().getClass());
        assertTrue(sut.getAsyncExecutor() instanceof ThreadPoolTaskExecutor);
    }

    @Test
    void getAsyncUncaughtExceptionHandler_logsWithoutThrowing() throws NoSuchMethodException {
        assertNotNull(sut.getAsyncUncaughtExceptionHandler());
        sut.getAsyncUncaughtExceptionHandler().handleUncaughtException(
                new RuntimeException("expected failure"),
                IntegrationEventsAsyncConfiguration.class.getMethod("integrationEventExecutor"),
                "event");
    }

}
