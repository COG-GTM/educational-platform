package com.educational.platform.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

/**
 * Provides retry classification for integration event handling.
 * Transient/recoverable exceptions are retried; business logic exceptions are not.
 */
@Component
public class IntegrationEventRetryHandler {

    private static final Logger log = LoggerFactory.getLogger(IntegrationEventRetryHandler.class);

    /**
     * Determines if an exception is transient and should be retried.
     */
    public static boolean isRetryable(Throwable throwable) {
        return throwable instanceof TransientDataAccessException
                || throwable instanceof OptimisticLockingFailureException
                || throwable instanceof PessimisticLockingFailureException;
    }

    /**
     * Retryable exception types for use with @Retryable annotation.
     */
    public static final Class<?>[] RETRYABLE_EXCEPTIONS = {
            TransientDataAccessException.class,
            OptimisticLockingFailureException.class,
            PessimisticLockingFailureException.class
    };
}
