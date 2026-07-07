package com.educational.platform;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configures asynchronous processing of integration events: a bounded thread pool with back-pressure
 * via {@link ThreadPoolExecutor.CallerRunsPolicy}, logging of exceptions escaping async event listeners,
 * and support for {@code @Retryable} on listener methods.
 */
@Configuration
@EnableResilientMethods
public class IntegrationEventsAsyncConfiguration implements AsyncConfigurer {

    public static final String INTEGRATION_EVENT_EXECUTOR = "integrationEventExecutor";

    private static final Logger logger = LoggerFactory.getLogger(IntegrationEventsAsyncConfiguration.class);

    @Bean(INTEGRATION_EVENT_EXECUTOR)
    public ThreadPoolTaskExecutor integrationEventExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("integration-event-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return integrationEventExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                logger.error("Integration event processing failed in [{}] with arguments {}", method, params, ex);
    }

}
