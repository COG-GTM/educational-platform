package com.educational.platform.config;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Integration test verifying that non-retryable exceptions in @Async+@Retryable handlers
 * are routed to the {@link AsyncUncaughtExceptionHandler} instead of @Recover.
 *
 * This validates the critical invariant: non-retryable exceptions bypass @Recover and
 * propagate to the async uncaught exception handler (since @Async swallows the exception
 * from the caller's perspective).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AsyncNonRetryableExceptionTest.TestConfig.class)
class AsyncNonRetryableExceptionTest {

    @Autowired
    private TestAsyncRetryHandler handler;

    @Autowired
    private TestCommandHandler commandHandler;

    @Autowired
    private FailedIntegrationEventRepository failedEventRepository;

    @Autowired
    private AtomicInteger invocationCounter;

    @Autowired
    private CopyOnWriteArrayList<Throwable> uncaughtExceptions;

    @Autowired
    private AtomicReference<CountDownLatch> latchRef;

    @BeforeEach
    void setUp() {
        Mockito.reset(commandHandler, failedEventRepository);
        invocationCounter.set(0);
        uncaughtExceptions.clear();
        latchRef.set(new CountDownLatch(1));
    }

    @Test
    void nonRetryableException_routesToAsyncUncaughtExceptionHandler() throws Exception {
        // given - handler throws non-retryable exception
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new IllegalArgumentException("business validation failed");
        }).when(commandHandler).handle(any());

        // when
        handler.handleEvent("test-event-1");

        // then - wait for async completion via the uncaught exception handler
        assertThat(latchRef.get().await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(invocationCounter.get()).isEqualTo(1);
        assertThat(uncaughtExceptions).hasSize(1);
        // ExhaustedRetryException wraps the non-retryable exception
        assertThat(uncaughtExceptions.get(0).getCause())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("business validation failed");
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void nonRetryableDataIntegrityViolation_routesToUncaughtHandler_notRecover() throws Exception {
        // given - DataIntegrityViolationException is DataAccessException but NOT retryable
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new DataIntegrityViolationException("unique constraint violated");
        }).when(commandHandler).handle(any());

        // when
        handler.handleEvent("test-event-2");

        // then
        assertThat(latchRef.get().await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(invocationCounter.get()).isEqualTo(1);
        assertThat(uncaughtExceptions).hasSize(1);
        assertThat(uncaughtExceptions.get(0).getCause())
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void retryableException_routesToRecoverMethod_notUncaughtHandler() throws Exception {
        // given - retryable exception should exhaust retries then call @Recover
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("connection timeout");
        }).when(commandHandler).handle(any());

        doAnswer(invocation -> {
            latchRef.get().countDown();
            return null;
        }).when(failedEventRepository).save(any(FailedIntegrationEventRecord.class));

        // when
        handler.handleEvent("test-event-3");

        // then
        assertThat(latchRef.get().await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(invocationCounter.get()).isEqualTo(3);
        assertThat(uncaughtExceptions).isEmpty();
        verify(failedEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void mixedExceptions_retryableThenNonRetryable_routesToUncaughtHandler() throws Exception {
        // given - first attempt retryable, second non-retryable
        doAnswer(invocation -> {
            int count = invocationCounter.incrementAndGet();
            if (count == 1) {
                throw new OptimisticLockingFailureException("retryable first");
            }
            throw new IllegalStateException("non-retryable second");
        }).when(commandHandler).handle(any());

        // when
        handler.handleEvent("test-event-4");

        // then - non-retryable exception escapes to uncaught handler
        assertThat(latchRef.get().await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(invocationCounter.get()).isEqualTo(2);
        assertThat(uncaughtExceptions).hasSize(1);
        assertThat(uncaughtExceptions.get(0))
                .isInstanceOf(ExhaustedRetryException.class);
        assertThat(uncaughtExceptions.get(0).getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("non-retryable second");
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void concurrentEvents_isolatedRetryAndRecovery() throws Exception {
        // given - two events: one succeeds, one exhausts retries
        var completionLatch = new CountDownLatch(2);
        var startBarrier = new CountDownLatch(1);

        doAnswer(invocation -> {
            startBarrier.await(5, TimeUnit.SECONDS);
            String event = (String) invocation.getArgument(0);
            invocationCounter.incrementAndGet();
            if (event.equals("fail-event")) {
                throw new QueryTimeoutException("always fails");
            }
            completionLatch.countDown();
            return null;
        }).when(commandHandler).handle(any());

        doAnswer(invocation -> {
            completionLatch.countDown();
            return null;
        }).when(failedEventRepository).save(any(FailedIntegrationEventRecord.class));

        // when - dispatch both events concurrently
        handler.handleEvent("success-event");
        handler.handleEvent("fail-event");
        startBarrier.countDown();

        // then - both complete independently
        assertThat(completionLatch.await(15, TimeUnit.SECONDS)).isTrue();
        // success-event: 1 invocation, fail-event: 3 invocations (retries) = 4 total
        assertThat(invocationCounter.get()).isEqualTo(4);
        verify(failedEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    static class TestAsyncRetryHandler {
        private final TestCommandHandler commandHandler;
        private final FailedIntegrationEventRepository failedEventRepository;

        TestAsyncRetryHandler(TestCommandHandler commandHandler, FailedIntegrationEventRepository failedEventRepository) {
            this.commandHandler = commandHandler;
            this.failedEventRepository = failedEventRepository;
        }

        @Async
        @Retryable(retryFor = {TransientDataAccessException.class, OptimisticLockingFailureException.class, PessimisticLockingFailureException.class},
                   maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
        public void handleEvent(String event) {
            try {
                commandHandler.handle(event);
            } catch (Exception e) {
                throw e;
            }
        }

        @Recover
        public void recover(TransientDataAccessException e, String event) {
            failedEventRepository.save(new FailedIntegrationEventRecord(
                    event.getClass().getName(),
                    event,
                    e.getMessage() != null ? e.getMessage() : e.getClass().getName(),
                    3
            ));
        }
    }

    interface TestCommandHandler {
        void handle(String event);
    }

    @Configuration
    @EnableAsync(order = Ordered.HIGHEST_PRECEDENCE + 100, proxyTargetClass = true)
    @EnableRetry(order = Ordered.HIGHEST_PRECEDENCE + 1, proxyTargetClass = true)
    static class TestConfig implements AsyncConfigurer {

        // Static holders initialized before AsyncConfigurer methods are invoked
        private static final CopyOnWriteArrayList<Throwable> UNCAUGHT_EXCEPTIONS = new CopyOnWriteArrayList<>();
        private static final AtomicReference<CountDownLatch> LATCH_REF = new AtomicReference<>(new CountDownLatch(1));

        @Bean
        AtomicInteger invocationCounter() {
            return new AtomicInteger(0);
        }

        @Bean
        CopyOnWriteArrayList<Throwable> uncaughtExceptions() {
            return UNCAUGHT_EXCEPTIONS;
        }

        @Bean
        AtomicReference<CountDownLatch> latchRef() {
            return LATCH_REF;
        }

        @Bean
        TestCommandHandler testCommandHandler() {
            return mock(TestCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedIntegrationEventRepository() {
            return mock(FailedIntegrationEventRepository.class);
        }

        @Bean
        TestAsyncRetryHandler testAsyncRetryHandler(TestCommandHandler commandHandler,
                                                     FailedIntegrationEventRepository repository) {
            return new TestAsyncRetryHandler(commandHandler, repository);
        }

        @Override
        @Bean
        public Executor getAsyncExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(4);
            executor.setMaxPoolSize(8);
            executor.setQueueCapacity(100);
            executor.setThreadNamePrefix("test-async-nonretry-");
            executor.initialize();
            return executor;
        }

        @Override
        public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
            return (ex, method, params) -> {
                UNCAUGHT_EXCEPTIONS.add(ex);
                LATCH_REF.get().countDown();
            };
        }
    }
}
