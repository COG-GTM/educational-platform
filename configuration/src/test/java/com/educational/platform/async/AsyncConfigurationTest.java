package com.educational.platform.async;

import com.educational.platform.common.async.AsyncConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AsyncConfigurationTest {

    @Test
    void integrationEventExecutor_hasExpectedConfiguration() {
        final AsyncConfiguration asyncConfiguration = new AsyncConfiguration();
        final ThreadPoolTaskExecutor executor = asyncConfiguration.integrationEventExecutor();
        executor.initialize();

        assertEquals(4, executor.getCorePoolSize());
        assertEquals(8, executor.getMaxPoolSize());
        assertEquals("integration-event-", executor.getThreadNamePrefix());
        assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class, executor.getThreadPoolExecutor().getRejectedExecutionHandler());
        assertNotNull(asyncConfiguration.getAsyncUncaughtExceptionHandler());
    }
}
