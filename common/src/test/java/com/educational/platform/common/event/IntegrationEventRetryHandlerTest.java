package com.educational.platform.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationEventRetryHandlerTest {

    @Test
    void createRetryTemplate_returnsNonNullTemplate() {
        // given
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
                DataAccessResourceFailureException.class, true,
                ObjectOptimisticLockingFailureException.class, true
        );

        // when
        RetryTemplate template = IntegrationEventRetryHandler.createRetryTemplate(retryableExceptions);

        // then
        assertThat(template).isNotNull();
    }

    @Test
    void createRetryTemplate_retriesOnConfiguredException() {
        // given
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
                DataAccessResourceFailureException.class, true
        );
        RetryTemplate template = IntegrationEventRetryHandler.createRetryTemplate(retryableExceptions);
        AtomicInteger attempts = new AtomicInteger(0);

        // when
        assertThatThrownBy(() -> template.execute(context -> {
            attempts.incrementAndGet();
            throw new DataAccessResourceFailureException("DB error");
        })).isInstanceOf(DataAccessResourceFailureException.class);

        // then - default max attempts is 3
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void createRetryTemplate_doesNotRetryOnNonConfiguredException() {
        // given
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
                DataAccessResourceFailureException.class, true
        );
        RetryTemplate template = IntegrationEventRetryHandler.createRetryTemplate(retryableExceptions);
        AtomicInteger attempts = new AtomicInteger(0);

        // when
        assertThatThrownBy(() -> template.execute(context -> {
            attempts.incrementAndGet();
            throw new IllegalArgumentException("Business error");
        })).isInstanceOf(IllegalArgumentException.class);

        // then - should not retry
        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    void createRetryTemplate_succeedsAfterTransientFailure() {
        // given
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
                DataAccessResourceFailureException.class, true
        );
        RetryTemplate template = IntegrationEventRetryHandler.createRetryTemplate(retryableExceptions);
        AtomicInteger attempts = new AtomicInteger(0);

        // when
        String result = template.execute(context -> {
            if (attempts.incrementAndGet() < 3) {
                throw new DataAccessResourceFailureException("transient");
            }
            return "success";
        });

        // then
        assertThat(result).isEqualTo("success");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void createRetryTemplate_emptyRetryableExceptions_neverRetries() {
        // given
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of();
        RetryTemplate template = IntegrationEventRetryHandler.createRetryTemplate(retryableExceptions);
        AtomicInteger attempts = new AtomicInteger(0);

        // when
        assertThatThrownBy(() -> template.execute(context -> {
            attempts.incrementAndGet();
            throw new DataAccessResourceFailureException("DB error");
        })).isInstanceOf(DataAccessResourceFailureException.class);

        // then
        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    void executeWithRetry_successfulExecution_returnsResult() throws Exception {
        // given
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
                DataAccessResourceFailureException.class, true
        );
        RetryTemplate template = IntegrationEventRetryHandler.createRetryTemplate(retryableExceptions);

        // when
        String result = IntegrationEventRetryHandler.executeWithRetry(
                template, "test-event", context -> "completed");

        // then
        assertThat(result).isEqualTo("completed");
    }

    @Test
    void executeWithRetry_allRetriesExhausted_throwsOriginalException() {
        // given
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
                DataAccessResourceFailureException.class, true
        );
        RetryTemplate template = IntegrationEventRetryHandler.createRetryTemplate(retryableExceptions);

        // when/then
        assertThatThrownBy(() -> IntegrationEventRetryHandler.executeWithRetry(
                template, "test-event", context -> {
                    throw new DataAccessResourceFailureException("persistent failure");
                }))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessage("persistent failure");
    }

    @Test
    void executeWithRetry_retriesAndSucceeds() throws Exception {
        // given
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
                DataAccessResourceFailureException.class, true
        );
        RetryTemplate template = IntegrationEventRetryHandler.createRetryTemplate(retryableExceptions);
        AtomicInteger attempts = new AtomicInteger(0);

        // when
        String result = IntegrationEventRetryHandler.executeWithRetry(
                template, "test-event", context -> {
                    if (attempts.incrementAndGet() < 2) {
                        throw new DataAccessResourceFailureException("transient");
                    }
                    return "recovered";
                });

        // then
        assertThat(result).isEqualTo("recovered");
        assertThat(attempts.get()).isEqualTo(2);
    }
}
