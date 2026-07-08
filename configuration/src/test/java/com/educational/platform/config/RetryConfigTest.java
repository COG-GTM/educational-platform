package com.educational.platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

import com.educational.platform.common.retry.IntegrationEventRetryPolicy;

class RetryConfigTest {

    private final SampleHandler handler = mock(SampleHandler.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RetryConfig.class, SampleConfiguration.class)
            .withBean(SampleHandler.class, () -> handler);

    @Test
    void retryConfigIsSpringConfigurationWithRetryEnabled() {
        // then
        assertThat(RetryConfig.class.isAnnotationPresent(Configuration.class)).isTrue();
        assertThat(RetryConfig.class.isAnnotationPresent(EnableRetry.class)).isTrue();
    }

    @Test
    void retryConfigActivatesRetryInterception_transientFailureIsRetriedUntilSuccess() {
        contextRunner.run(context -> {
            // given
            final RetryableSample sample = context.getBean(RetryableSample.class);
            final AtomicInteger attempts = new AtomicInteger();
            doAnswer(invocation -> {
                if (attempts.getAndIncrement() < IntegrationEventRetryPolicy.MAX_ATTEMPTS - 1) {
                    throw new IllegalStateException("transient");
                }
                return null;
            }).when(handler).handle();

            // when / then
            assertThatCode(sample::invoke).doesNotThrowAnyException();
            verify(handler, times(IntegrationEventRetryPolicy.MAX_ATTEMPTS)).handle();
            verify(handler, never()).recovered();
        });
    }

    @Test
    void retryConfigActivatesRetryInterception_exhaustionRoutesToRecover() {
        contextRunner.run(context -> {
            // given
            final RetryableSample sample = context.getBean(RetryableSample.class);
            doThrow(new IllegalStateException("boom")).when(handler).handle();

            // when / then
            assertThatCode(sample::invoke).doesNotThrowAnyException();
            verify(handler, times(IntegrationEventRetryPolicy.MAX_ATTEMPTS)).handle();
            verify(handler, times(1)).recovered();
        });
    }

    interface SampleHandler {

        void handle();

        void recovered();
    }

    @Configuration
    static class SampleConfiguration {

        @Bean
        RetryableSample retryableSample(SampleHandler handler) {
            return new RetryableSample(handler);
        }
    }

    static class RetryableSample {

        private final SampleHandler handler;

        RetryableSample(SampleHandler handler) {
            this.handler = handler;
        }

        @Retryable(
                retryFor = Exception.class,
                maxAttempts = IntegrationEventRetryPolicy.MAX_ATTEMPTS,
                backoff = @Backoff(
                        delay = IntegrationEventRetryPolicy.INITIAL_DELAY_MS,
                        multiplier = IntegrationEventRetryPolicy.MULTIPLIER,
                        maxDelay = IntegrationEventRetryPolicy.MAX_DELAY_MS))
        public void invoke() {
            handler.handle();
        }

        @Recover
        public void recover(Exception ex) {
            handler.recovered();
        }
    }
}
