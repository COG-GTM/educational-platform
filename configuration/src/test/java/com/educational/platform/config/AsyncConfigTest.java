package com.educational.platform.config;

import java.lang.reflect.Method;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(AsyncConfig.LoggingAsyncUncaughtExceptionHandler.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    void shouldLogMethodNameParametersAndExceptionAtErrorLevel() throws NoSuchMethodException {
        final AsyncUncaughtExceptionHandler handler = new AsyncConfig().getAsyncUncaughtExceptionHandler();
        final Method method = SampleAsyncBean.class.getMethod("handleEvent", String.class);
        final RuntimeException exception = new IllegalStateException("boom");

        handler.handleUncaughtException(exception, method, "payload-123");

        final List<ILoggingEvent> events = listAppender.list;
        assertFalse(events.isEmpty(), "expected an error log event to be recorded");

        final ILoggingEvent event = events.get(0);
        assertEquals(Level.ERROR, event.getLevel());

        final String message = event.getFormattedMessage();
        assertTrue(message.contains("handleEvent"), "log should contain the failing method name");
        assertTrue(message.contains("payload-123"), "log should contain the method parameters");

        assertNotNull(event.getThrowableProxy(), "exception should be attached to the log event");
        assertEquals(IllegalStateException.class.getName(), event.getThrowableProxy().getClassName());
    }

    @Test
    void shouldHandleNullParamsWithoutThrowing() throws NoSuchMethodException {
        final AsyncUncaughtExceptionHandler handler = new AsyncConfig().getAsyncUncaughtExceptionHandler();
        final Method method = SampleAsyncBean.class.getMethod("handleEvent", String.class);

        handler.handleUncaughtException(new IllegalStateException("boom"), method, (Object[]) null);

        final List<ILoggingEvent> events = listAppender.list;
        assertFalse(events.isEmpty(), "expected an error log event even with null params");
        assertEquals(Level.ERROR, events.get(0).getLevel());
        assertTrue(events.get(0).getFormattedMessage().contains("handleEvent"));
    }

    @Test
    void shouldProvideDedicatedThreadPoolExecutorForIntegrationEvents() {
        final Executor executor = new AsyncConfig().getAsyncExecutor();

        assertNotNull(executor, "async executor must be configured");
        final ThreadPoolTaskExecutor poolExecutor = assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
        assertEquals(2, poolExecutor.getCorePoolSize());
        assertEquals(10, poolExecutor.getMaxPoolSize());
        assertEquals(100, poolExecutor.getQueueCapacity());
        assertEquals("integration-event-", poolExecutor.getThreadNamePrefix());
    }

    @Test
    void shouldRunTasksOnTheDedicatedIntegrationEventPool() throws Exception {
        final ThreadPoolTaskExecutor poolExecutor =
                (ThreadPoolTaskExecutor) new AsyncConfig().getAsyncExecutor();
        poolExecutor.initialize();
        try {
            final java.util.concurrent.CompletableFuture<String> future = new java.util.concurrent.CompletableFuture<>();
            poolExecutor.execute(() -> future.complete(Thread.currentThread().getName()));

            final String threadName = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(threadName.startsWith("integration-event-"),
                    "async tasks should run on the dedicated integration-event pool");
        } finally {
            poolExecutor.destroy();
        }
    }

    @Test
    void integrationEventExecutorBeanIsConfiguredAndConsistentWithAsyncExecutor() {
        final AsyncConfig config = new AsyncConfig();

        final ThreadPoolTaskExecutor bean = config.integrationEventExecutor();

        assertNotNull(bean, "integration-event executor bean must be configured");
        assertEquals(2, bean.getCorePoolSize());
        assertEquals(10, bean.getMaxPoolSize());
        assertEquals(100, bean.getQueueCapacity());
        assertEquals("integration-event-", bean.getThreadNamePrefix());

        final Executor asyncExecutor = config.getAsyncExecutor();
        final ThreadPoolTaskExecutor asyncPool = assertInstanceOf(ThreadPoolTaskExecutor.class, asyncExecutor);
        assertEquals(bean.getCorePoolSize(), asyncPool.getCorePoolSize());
        assertEquals(bean.getMaxPoolSize(), asyncPool.getMaxPoolSize());
        assertEquals(bean.getQueueCapacity(), asyncPool.getQueueCapacity());
        assertEquals(bean.getThreadNamePrefix(), asyncPool.getThreadNamePrefix());
    }

    @Test
    void shouldWaitForInFlightTaskToCompleteOnShutdown() throws Exception {
        final ThreadPoolTaskExecutor executor = new AsyncConfig().integrationEventExecutor();
        executor.initialize();

        final java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean(false);
        executor.execute(() -> {
            started.countDown();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            completed.set(true);
        });

        assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS), "submitted task should start running");

        // destroy() blocks until in-flight tasks finish because
        // waitForTasksToCompleteOnShutdown=true / awaitTerminationSeconds=30.
        executor.destroy();

        assertTrue(completed.get(),
                "graceful shutdown should wait for in-flight integration-event handlers to finish");
    }

    static class SampleAsyncBean {
        public void handleEvent(String payload) {
        }
    }
}
