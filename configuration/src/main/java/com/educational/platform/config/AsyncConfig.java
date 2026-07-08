package com.educational.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configures the executor used to process integration events asynchronously.
 *
 * <p>A dedicated {@link ThreadPoolTaskExecutor} with a bounded queue and a
 * {@link ThreadPoolExecutor.CallerRunsPolicy} rejection handler replaces the default
 * {@code SimpleAsyncTaskExecutor} (which spawns an unbounded number of threads). The
 * {@link AsyncUncaughtExceptionHandler} guarantees that exceptions thrown from
 * {@code void} {@code @Async} {@code @EventListener} methods are logged instead of being
 * silently swallowed.
 */
@Configuration
@EnableRetry
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Bean name of the executor used by integration event listeners via
     * {@code @Async("integrationEventExecutor")}.
     */
    public static final String INTEGRATION_EVENT_EXECUTOR = "integrationEventExecutor";

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Bounded executor dedicated to integration event processing.
     *
     * @return configured thread pool task executor
     */
    @Bean(name = INTEGRATION_EVENT_EXECUTOR)
    public ThreadPoolTaskExecutor integrationEventExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("integration-event-");
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
        return (throwable, method, params) -> log.error(
                "Unexpected exception in async method '{}' with parameters {}",
                method.getName(), Arrays.toString(params), throwable);
    }
}
