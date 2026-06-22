package com.educational.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;

@EnableAsync(order = Ordered.HIGHEST_PRECEDENCE)
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "integrationEventExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("integration-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new IntegrationEventAsyncExceptionHandler();
    }

    private static class IntegrationEventAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(IntegrationEventAsyncExceptionHandler.class);

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("Uncaught async exception in method '{}' with parameters {}",
                    method.getName(), Arrays.toString(params), ex);
        }
    }
}
