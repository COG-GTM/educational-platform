package com.educational.platform.common.event;

import com.educational.platform.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationEventRetryHandlerTest {

    @Test
    void isRetryable_transientDataAccessException_returnsTrue() {
        // given
        final Throwable exception = new QueryTimeoutException("timeout");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_optimisticLockingFailureException_returnsTrue() {
        // given
        final Throwable exception = new OptimisticLockingFailureException("optimistic lock");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_pessimisticLockingFailureException_returnsTrue() {
        // given
        final Throwable exception = new PessimisticLockingFailureException("pessimistic lock");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_runtimeException_returnsFalse() {
        // given
        final Throwable exception = new RuntimeException("generic error");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_resourceNotFoundException_returnsFalse() {
        // given
        final Throwable exception = new ResourceNotFoundException("not found");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_illegalArgumentException_returnsFalse() {
        // given
        final Throwable exception = new IllegalArgumentException("bad argument");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_nullPointerException_returnsFalse() {
        // given
        final Throwable exception = new NullPointerException("null");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void retryableExceptions_containsExactlyThreeExpectedTypes() {
        // then
        assertThat(IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS)
                .hasSize(3)
                .containsExactly(
                        TransientDataAccessException.class,
                        OptimisticLockingFailureException.class,
                        PessimisticLockingFailureException.class
                );
    }

}
