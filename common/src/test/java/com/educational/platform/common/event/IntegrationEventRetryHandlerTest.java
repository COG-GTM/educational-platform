package com.educational.platform.common.event;

import com.educational.platform.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationEventRetryHandlerTest {

    private final IntegrationEventRetryHandler retryHandler = new IntegrationEventRetryHandler();

    @Test
    void retriesTransientException_upToMaxAttempts() {
        final AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> retryHandler.execute(context -> {
            attempts.incrementAndGet();
            throw new OptimisticLockingFailureException("transient");
        })).isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(attempts.get()).isEqualTo(IntegrationEventRetryHandler.MAX_ATTEMPTS);
    }

    @Test
    void doesNotRetryBusinessException() {
        final AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> retryHandler.execute(context -> {
            attempts.incrementAndGet();
            throw new ResourceNotFoundException("not found");
        })).isInstanceOf(ResourceNotFoundException.class);

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    void returnsResult_whenCallbackSucceeds() throws Exception {
        final String result = retryHandler.execute(context -> "ok");

        assertThat(result).isEqualTo("ok");
    }
}
