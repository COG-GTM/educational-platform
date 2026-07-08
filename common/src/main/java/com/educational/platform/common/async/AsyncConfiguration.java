package com.educational.platform.common.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async configuration for integration events.
 */
@Configuration
@EnableRetry
public class AsyncConfiguration implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfiguration.class);

    @Bean("integrationEventExecutor")
    public ThreadPoolTaskExecutor integrationEventExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("integration-event-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return integrationEventExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this::logAsyncException;
    }

    private void logAsyncException(Throwable throwable, Method method, Object... params) {
        logger.error("Uncaught async exception in method {} with params {}", method, Arrays.toString(params), throwable);
    }
}
