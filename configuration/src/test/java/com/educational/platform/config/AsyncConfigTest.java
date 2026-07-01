package com.educational.platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.educational.platform.config.AsyncConfig.LoggingAsyncUncaughtExceptionHandler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class AsyncConfigTest {

    private AsyncConfig asyncConfig;
    private LoggingAsyncUncaughtExceptionHandler handler;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        asyncConfig = new AsyncConfig();
        handler = new LoggingAsyncUncaughtExceptionHandler();
        logger = (Logger) LoggerFactory.getLogger(LoggingAsyncUncaughtExceptionHandler.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void getAsyncExecutorReturnsConfiguredThreadPoolTaskExecutor() {
        final Executor executor = asyncConfig.getAsyncExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        final ThreadPoolTaskExecutor poolExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(poolExecutor.getCorePoolSize()).isEqualTo(2);
        assertThat(poolExecutor.getMaxPoolSize()).isEqualTo(10);
        assertThat(poolExecutor.getQueueCapacity()).isEqualTo(100);
        assertThat(poolExecutor.getThreadNamePrefix()).isEqualTo("integration-event-");
    }

    @Test
    void getAsyncExecutorReturnsInitializedExecutorThatRunsTasksOnPrefixedThread() throws InterruptedException {
        final ThreadPoolTaskExecutor poolExecutor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        final AtomicReference<String> threadName = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        // Succeeds only if initialize() was called (an uninitialized executor throws on execute()).
        poolExecutor.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(threadName.get()).startsWith("integration-event-");
        poolExecutor.shutdown();
    }

    @Test
    void getAsyncExecutorReturnsIndependentExecutorInstances() {
        assertThat(asyncConfig.getAsyncExecutor()).isNotSameAs(asyncConfig.getAsyncExecutor());
    }

    @Test
    void getAsyncUncaughtExceptionHandlerReturnsLoggingHandler() {
        final AsyncUncaughtExceptionHandler exceptionHandler = asyncConfig.getAsyncUncaughtExceptionHandler();

        assertThat(exceptionHandler)
                .isNotNull()
                .isInstanceOf(LoggingAsyncUncaughtExceptionHandler.class);
    }

    @Test
    void logsErrorWithMethodNameParametersAndExceptionAndDoesNotThrow() throws NoSuchMethodException {
        final Method method = SampleAsyncTarget.class.getDeclaredMethod("doWork", String.class);
        final RuntimeException exception = new IllegalStateException("boom");

        assertThatCode(() -> handler.handleUncaughtException(exception, method, "arg-value"))
                .doesNotThrowAnyException();

        final List<ILoggingEvent> events = appender.list;
        assertThat(events).hasSize(1);
        final ILoggingEvent event = events.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage())
                .contains("doWork")
                .contains("arg-value")
                .contains("boom");
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getThrowableProxy().getClassName()).isEqualTo(IllegalStateException.class.getName());
    }

    @Test
    void logsErrorWithMultipleParametersRendered() throws NoSuchMethodException {
        final Method method = SampleAsyncTarget.class.getDeclaredMethod("doWork", String.class, int.class);

        handler.handleUncaughtException(new RuntimeException("kaboom"), method, "first", 42);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("[first, 42]");
    }

    @Test
    void logsErrorWithNoParametersRendersEmptyArray() throws NoSuchMethodException {
        final Method method = SampleAsyncTarget.class.getDeclaredMethod("noArgs");

        handler.handleUncaughtException(new RuntimeException("no-args"), method);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage())
                .contains("noArgs")
                .contains("[]");
    }

    @Test
    void logsErrorWhenExceptionMessageIsNull() throws NoSuchMethodException {
        final Method method = SampleAsyncTarget.class.getDeclaredMethod("doWork", String.class);

        assertThatCode(() -> handler.handleUncaughtException(new NullPointerException(), method, "value"))
                .doesNotThrowAnyException();

        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getThrowableProxy().getClassName()).isEqualTo(NullPointerException.class.getName());
    }

    @SuppressWarnings("unused")
    private static final class SampleAsyncTarget {
        void doWork(String value) {
            // no-op target used only to obtain a Method reference
        }

        void doWork(String value, int count) {
            // no-op overload used only to obtain a multi-parameter Method reference
        }

        void noArgs() {
            // no-op target used only to obtain a no-parameter Method reference
        }
    }
}
