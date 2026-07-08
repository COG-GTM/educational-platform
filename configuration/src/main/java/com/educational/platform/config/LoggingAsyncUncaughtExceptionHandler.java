package com.educational.platform.config;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

/**
 * Logs exceptions thrown from {@code void} {@code @Async} methods (such as async
 * {@code @EventListener} integration-event consumers) which would otherwise be
 * silently swallowed by Spring.
 */
@Component
public class LoggingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingAsyncUncaughtExceptionHandler.class);

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Unhandled exception in async method '{}.{}' with params {}",
                method.getDeclaringClass().getName(),
                method.getName(),
                Arrays.toString(params),
                ex);
    }
}
