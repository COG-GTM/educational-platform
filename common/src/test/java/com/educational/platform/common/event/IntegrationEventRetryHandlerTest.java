package com.educational.platform.common.event;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationEventRetryHandlerTest {

    private final IntegrationEventRetryHandler handler = new IntegrationEventRetryHandler();

    @Test
    void exposesConfiguredRetryConstants() {
        // expect
        assertThat(IntegrationEventRetryHandler.MAX_ATTEMPTS).isEqualTo(3);
        assertThat(IntegrationEventRetryHandler.INITIAL_INTERVAL_MS).isEqualTo(500L);
        assertThat(IntegrationEventRetryHandler.BACKOFF_MULTIPLIER).isEqualTo(2.0);
    }

    @Test
    void transientExceptionsContainDataAccessAndOptimisticLockingTypes() {
        // expect
        assertThat(IntegrationEventRetryHandler.TRANSIENT_EXCEPTIONS)
                .containsExactly(DataAccessException.class, OptimisticLockingFailureException.class);
    }

    @Test
    void retryTemplate_isNotNullAndStable() {
        // when
        final RetryTemplate first = handler.retryTemplate();
        final RetryTemplate second = handler.retryTemplate();

        // then
        assertThat(first).isNotNull().isSameAs(second);
    }

    @Test
    void retryTemplate_doesNotRetrySuccessfulCall() throws Exception {
        // given
        final AtomicInteger invocations = new AtomicInteger();

        // when
        final String result = handler.retryTemplate().execute(context -> {
            invocations.incrementAndGet();
            return "ok";
        });

        // then
        assertThat(result).isEqualTo("ok");
        assertThat(invocations).hasValue(1);
    }

    @Test
    void retryTemplate_retriesTransientFailureUntilMaxAttemptsThenRethrows() {
        // given
        final AtomicInteger invocations = new AtomicInteger();

        // when / then
        assertThatThrownBy(() -> handler.retryTemplate().execute(context -> {
            invocations.incrementAndGet();
            throw new OptimisticLockingFailureException("conflict");
        })).isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(invocations).hasValue(IntegrationEventRetryHandler.MAX_ATTEMPTS);
    }

    @Test
    void retryTemplate_recoversWhenTransientFailureSucceedsBeforeMaxAttempts() throws Exception {
        // given
        final AtomicInteger invocations = new AtomicInteger();

        // when
        final String result = handler.retryTemplate().execute(context -> {
            if (invocations.incrementAndGet() < 2) {
                throw new EmptyResultDataAccessException(1);
            }
            return "recovered";
        });

        // then
        assertThat(result).isEqualTo("recovered");
        assertThat(invocations).hasValue(2);
    }

    @Test
    void retryTemplate_retriesSubclassesOfTransientExceptions() {
        // given - EmptyResultDataAccessException is a subclass of DataAccessException
        final AtomicInteger invocations = new AtomicInteger();

        // when / then
        assertThatThrownBy(() -> handler.retryTemplate().execute(context -> {
            invocations.incrementAndGet();
            throw new EmptyResultDataAccessException(1);
        })).isInstanceOf(EmptyResultDataAccessException.class);

        assertThat(invocations).hasValue(IntegrationEventRetryHandler.MAX_ATTEMPTS);
    }

    @Test
    void retryTemplate_doesNotRetryBusinessExceptions() {
        // given
        final AtomicInteger invocations = new AtomicInteger();

        // when / then
        assertThatThrownBy(() -> handler.retryTemplate().execute(context -> {
            invocations.incrementAndGet();
            throw new IllegalStateException("business rule violated");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(invocations).hasValue(1);
    }
}
