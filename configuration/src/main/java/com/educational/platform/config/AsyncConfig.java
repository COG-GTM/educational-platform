package com.educational.platform.config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configures asynchronous integration-event processing and retry support.
 *
 * <p>Provides a dedicated thread pool for {@code @Async} integration-event handlers,
 * enables Spring Retry ({@code @Retryable}/{@code @Recover}) and installs a custom
 * {@link AsyncUncaughtExceptionHandler} that logs full context for exceptions thrown
 * from fire-and-forget async methods (which would otherwise be silently lost).
 */
@Configuration
@EnableAsync
@EnableRetry
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("integration-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new LoggingAsyncUncaughtExceptionHandler();
    }

    /**
     * Logs uncaught exceptions from {@code @Async} methods together with the failing
     * method name, its parameters and the full stack trace.
     */
    static class LoggingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(LoggingAsyncUncaughtExceptionHandler.class);

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            logger.error("Uncaught exception in async method '{}' invoked with parameters {}",
                    method.getName(), Arrays.toString(params), ex);
        }
    }
}
