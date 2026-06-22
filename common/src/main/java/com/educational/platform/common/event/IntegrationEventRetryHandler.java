package com.educational.platform.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

/**
 * Provides a reusable retry template for integration event handlers.
 * Retries only on transient/recoverable exceptions with exponential backoff.
 */
public final class IntegrationEventRetryHandler {

    private static final Logger log = LoggerFactory.getLogger(IntegrationEventRetryHandler.class);

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_INITIAL_INTERVAL = 500L;
    private static final double DEFAULT_MULTIPLIER = 2.0;

    private IntegrationEventRetryHandler() {
    }

    /**
     * Creates a RetryTemplate configured for integration event handling with exponential backoff.
     * Retries only on the specified transient exception types.
     */
    public static RetryTemplate createRetryTemplate(Map<Class<? extends Throwable>, Boolean> retryableExceptions) {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(DEFAULT_MAX_ATTEMPTS, retryableExceptions, true);
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(DEFAULT_INITIAL_INTERVAL);
        backOffPolicy.setMultiplier(DEFAULT_MULTIPLIER);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    /**
     * Executes the given action with retry logic. Logs retry attempts and exhaustion.
     */
    public static <T> T executeWithRetry(RetryTemplate retryTemplate, String eventDescription, RetryCallback<T, Exception> callback) throws Exception {
        return retryTemplate.execute(callback, context -> {
            log.error("All retries exhausted for event: {}. Attempt: {}", eventDescription, context.getRetryCount());
            throw context.getLastThrowable() instanceof Exception ex ? ex : new RuntimeException(context.getLastThrowable());
        });
    }
}
