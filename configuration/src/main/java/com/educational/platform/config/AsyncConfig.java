package com.educational.platform.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configures the {@link Executor} backing {@code @Async} integration-event consumers.
 *
 * <p>Without a custom executor Spring falls back to {@code SimpleAsyncTaskExecutor},
 * which spawns an unbounded new thread per task (no pooling, no back-pressure). This
 * configuration provides a bounded {@link ThreadPoolTaskExecutor} with a
 * {@link ThreadPoolExecutor.CallerRunsPolicy} rejection policy so that a saturated
 * queue applies back-pressure to the publisher rather than dropping work.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    public static final String INTEGRATION_EVENT_EXECUTOR = "integrationEventExecutor";

    static final int CORE_POOL_SIZE = 4;
    static final int MAX_POOL_SIZE = 8;
    static final int QUEUE_CAPACITY = 500;
    static final String THREAD_NAME_PREFIX = "integration-event-";

    private final AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler;

    public AsyncConfig(AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler) {
        this.asyncUncaughtExceptionHandler = asyncUncaughtExceptionHandler;
    }

    @Bean(INTEGRATION_EVENT_EXECUTOR)
    public ThreadPoolTaskExecutor integrationEventExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return integrationEventExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncUncaughtExceptionHandler;
    }
}
