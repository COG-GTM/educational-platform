package com.educational.platform.config;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
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
 * Integration test verifying that @Async and @Retryable work together correctly
 * when both are enabled with proper AOP advisor ordering.
 *
 * This test ensures:
 * - @Async dispatches handler execution to the configured thread pool
 * - @Retryable retries happen WITHIN the async thread (not on the caller's thread)
 * - @Recover is invoked in the same async thread after exhausting retries
 * - The AOP ordering (Async outer, Retry inner) functions correctly
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AsyncRetryOrderingIntegrationTest.AsyncRetryTestConfig.class)
class AsyncRetryOrderingIntegrationTest {

    @Autowired
    private TestAsyncRetryHandler handler;

    @Autowired
    private TestCommandHandler commandHandler;

    @Autowired
    private FailedIntegrationEventRepository failedEventRepository;

    @Autowired
    private AtomicInteger invocationCounter;

    @Autowired
    private CopyOnWriteArrayList<String> threadNames;

    @Autowired
    private AtomicReference<CountDownLatch> latchRef;

    @BeforeEach
    void setUp() {
        Mockito.reset(commandHandler, failedEventRepository);
        invocationCounter.set(0);
        threadNames.clear();
        latchRef.set(new CountDownLatch(1));
    }

    @Test
    void asyncRetry_retriesExecuteInAsyncThread_notCallerThread() throws Exception {
        // given
        final String callerThread = Thread.currentThread().getName();
        doAnswer(invocation -> {
            threadNames.add(Thread.currentThread().getName());
            invocationCounter.incrementAndGet();
            if (invocationCounter.get() < 3) {
                throw new QueryTimeoutException("transient failure " + invocationCounter.get());
            }
            latchRef.get().countDown();
            return null;
        }).when(commandHandler).handle(any());

        // when - @Async dispatches to thread pool, @Retryable retries within that thread
        handler.handleEvent("test-event-1");

        // then - wait for async completion
        assertThat(latchRef.get().await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(invocationCounter.get()).isEqualTo(3);

        // all retries happened in async thread(s), never on the caller thread
        assertThat(threadNames).isNotEmpty();
        threadNames.forEach(name -> {
            assertThat(name)
                    .as("Retry should execute in async thread pool, not caller thread '%s'", callerThread)
                    .startsWith("test-async-");
            assertThat(name).isNotEqualTo(callerThread);
        });

        // recovery should NOT have been called since the 3rd attempt succeeded
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void asyncRetry_recoveryExecutesInSameAsyncThread() throws Exception {
        // given
        doAnswer(invocation -> {
            threadNames.add(Thread.currentThread().getName());
            invocationCounter.incrementAndGet();
            throw new OptimisticLockingFailureException("lock failure " + invocationCounter.get());
        }).when(commandHandler).handle(any());

        doAnswer(invocation -> {
            threadNames.add("RECOVER:" + Thread.currentThread().getName());
            latchRef.get().countDown();
            return null;
        }).when(failedEventRepository).save(any(FailedIntegrationEventRecord.class));

        // when
        handler.handleEvent("test-event-2");

        // then
        assertThat(latchRef.get().await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(invocationCounter.get()).isEqualTo(3);

        // verify that recovery also ran in the async thread pool
        String recoverThread = threadNames.stream()
                .filter(n -> n.startsWith("RECOVER:"))
                .findFirst()
                .map(n -> n.substring("RECOVER:".length()))
                .orElseThrow(() -> new AssertionError("Recovery thread not recorded"));
        assertThat(recoverThread).startsWith("test-async-");
    }

    @Test
    void asyncRetry_allRetriesUseSameAsyncThread() throws Exception {
        // given - verify retries don't spawn new async tasks
        doAnswer(invocation -> {
            threadNames.add(Thread.currentThread().getName());
            int count = invocationCounter.incrementAndGet();
            if (count < 3) {
                throw new PessimisticLockingFailureException("lock " + count);
            }
            latchRef.get().countDown();
            return null;
        }).when(commandHandler).handle(any());

        // when
        handler.handleEvent("test-event-3");

        // then
        assertThat(latchRef.get().await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(threadNames).hasSize(3);

        // all 3 retries should execute in the same thread (retry is inner advisor)
        assertThat(threadNames.stream().distinct().count())
                .as("All retries should use the same async thread (retry is inner to async)")
                .isEqualTo(1);
    }

    @Test
    void asyncRetry_callerReturnsImmediately_doesNotBlockOnRetry() throws Exception {
        // given - handler retries 3 times with delay; caller should not wait
        doAnswer(invocation -> {
            threadNames.add(Thread.currentThread().getName());
            int count = invocationCounter.incrementAndGet();
            if (count < 3) {
                throw new QueryTimeoutException("timeout " + count);
            }
            latchRef.get().countDown();
            return null;
        }).when(commandHandler).handle(any());

        // when - capture time before and after the call
        long startTime = System.nanoTime();
        handler.handleEvent("test-event-4");
        long elapsed = System.nanoTime() - startTime;

        // then - caller returns almost immediately (within 500ms), retries happen in background
        assertThat(elapsed)
                .as("Caller must not block on retries; @Async should return immediately")
                .isLessThan(TimeUnit.MILLISECONDS.toNanos(500));

        // wait for async completion
        assertThat(latchRef.get().await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void asyncRetry_recoveryPersistsCorrectRecord() throws Exception {
        // given
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("async timeout");
        }).when(commandHandler).handle(any());

        doAnswer(invocation -> {
            latchRef.get().countDown();
            return null;
        }).when(failedEventRepository).save(any(FailedIntegrationEventRecord.class));

        // when
        handler.handleEvent("test-event-5");

        // then
        assertThat(latchRef.get().await(10, TimeUnit.SECONDS)).isTrue();
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        FailedIntegrationEventRecord record = captor.getValue();
        assertThat(record.getEventClassName()).isEqualTo(String.class.getName());
        assertThat(record.getEventPayload()).isEqualTo("test-event-5");
        assertThat(record.getExceptionMessage()).isEqualTo("async timeout");
        assertThat(record.getRetryCount()).isEqualTo(3);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    /**
     * Test handler mimicking the production handler pattern with @Async + @Retryable + @Recover.
     */
    static class TestAsyncRetryHandler {
        private final TestCommandHandler commandHandler;
        private final FailedIntegrationEventRepository failedEventRepository;

        TestAsyncRetryHandler(TestCommandHandler commandHandler, FailedIntegrationEventRepository failedEventRepository) {
            this.commandHandler = commandHandler;
            this.failedEventRepository = failedEventRepository;
        }

        @Async
        @Retryable(retryFor = {TransientDataAccessException.class, OptimisticLockingFailureException.class, PessimisticLockingFailureException.class},
                   maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
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
    @EnableAsync(order = Ordered.HIGHEST_PRECEDENCE)
    @EnableRetry(order = Ordered.HIGHEST_PRECEDENCE + 1)
    static class AsyncRetryTestConfig {

        @Bean
        AtomicInteger invocationCounter() {
            return new AtomicInteger(0);
        }

        @Bean
        CopyOnWriteArrayList<String> threadNames() {
            return new CopyOnWriteArrayList<>();
        }

        @Bean
        AtomicReference<CountDownLatch> latchRef() {
            return new AtomicReference<>(new CountDownLatch(1));
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

        @Bean
        Executor taskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(4);
            executor.setMaxPoolSize(8);
            executor.setQueueCapacity(100);
            executor.setThreadNamePrefix("test-async-");
            executor.initialize();
            return executor;
        }
    }
}
