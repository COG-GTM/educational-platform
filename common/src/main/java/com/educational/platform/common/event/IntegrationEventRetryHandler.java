package com.educational.platform.common.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * Reusable retry mechanism for integration-event handlers.
 *
 * <p>Exposes a {@link RetryTemplate} configured with a maximum of {@value #MAX_ATTEMPTS}
 * attempts and exponential backoff (initial {@value #INITIAL_INTERVAL_MS}ms, multiplier
 * {@value #BACKOFF_MULTIPLIER}). Only transient/recoverable infrastructure failures are
 * retried (see {@link #TRANSIENT_EXCEPTIONS}); business exceptions such as
 * {@code ResourceNotFoundException} are never retried because retrying them cannot succeed.
 *
 * <p>Handlers may use this template programmatically via {@link #retryTemplate()}, or
 * rely on the declarative {@code @Retryable}/{@code @Recover} annotations enabled by
 * {@code AsyncConfig}.
 */
@Component
public class IntegrationEventRetryHandler {

    public static final int MAX_ATTEMPTS = 3;
    public static final long INITIAL_INTERVAL_MS = 500L;
    public static final double BACKOFF_MULTIPLIER = 2.0;

    /**
     * Exception types (and their subclasses) treated as transient/recoverable and therefore retried.
     */
    public static final List<Class<? extends Throwable>> TRANSIENT_EXCEPTIONS = List.of(
            DataAccessException.class,
            OptimisticLockingFailureException.class);

    private final RetryTemplate retryTemplate;

    public IntegrationEventRetryHandler() {
        this.retryTemplate = buildRetryTemplate();
    }

    public RetryTemplate retryTemplate() {
        return retryTemplate;
    }

    private static RetryTemplate buildRetryTemplate() {
        final Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        TRANSIENT_EXCEPTIONS.forEach(exception -> retryableExceptions.put(exception, true));

        final SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(MAX_ATTEMPTS, retryableExceptions, true);

        final ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(INITIAL_INTERVAL_MS);
        backOffPolicy.setMultiplier(BACKOFF_MULTIPLIER);

        final RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);
        return template;
    }
}
