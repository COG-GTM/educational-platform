package com.educational.platform.common.event;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Reusable utility that exposes a {@link RetryTemplate} pre-configured for asynchronous
 * integration event handling.
 *
 * <p>Retries are performed only on transient / recoverable data-access failures
 * ({@link TransientDataAccessException}, {@link DataAccessResourceFailureException} and
 * {@link OptimisticLockingFailureException}). Business exceptions such as
 * {@link com.educational.platform.common.exception.ResourceNotFoundException} are deliberately
 * excluded so they propagate immediately without wasting retry attempts.</p>
 *
 * <p>Policy: max 3 attempts, exponential backoff starting at 500ms with a multiplier of 2.</p>
 */
@Component
public class IntegrationEventRetryHandler {

    public static final int MAX_ATTEMPTS = 3;
    public static final long INITIAL_BACKOFF_MS = 500L;
    public static final double BACKOFF_MULTIPLIER = 2.0;

    private final RetryTemplate retryTemplate;

    public IntegrationEventRetryHandler() {
        this.retryTemplate = buildRetryTemplate();
    }

    private static RetryTemplate buildRetryTemplate() {
        final RetryTemplate template = new RetryTemplate();

        final Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(TransientDataAccessException.class, true);
        retryableExceptions.put(DataAccessResourceFailureException.class, true);
        retryableExceptions.put(OptimisticLockingFailureException.class, true);

        final SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(MAX_ATTEMPTS, retryableExceptions, true);
        template.setRetryPolicy(retryPolicy);

        final ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(INITIAL_BACKOFF_MS);
        backOffPolicy.setMultiplier(BACKOFF_MULTIPLIER);
        template.setBackOffPolicy(backOffPolicy);

        return template;
    }

    /**
     * @return the configured {@link RetryTemplate}.
     */
    public RetryTemplate retryTemplate() {
        return retryTemplate;
    }

    /**
     * Executes the given callback with the configured retry policy.
     *
     * @param callback logic to execute
     * @param <T>      return type
     * @return the callback result
     * @throws Exception the last thrown exception once retries are exhausted
     */
    public <T> T execute(RetryCallback<T, Exception> callback) throws Exception {
        return retryTemplate.execute(callback);
    }
}
