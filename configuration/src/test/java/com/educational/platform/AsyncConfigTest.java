package com.educational.platform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class AsyncConfigTest {

    private AsyncConfig config;
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        config = new AsyncConfig();
        executor = config.integrationEventExecutor();
        executor.initialize();
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void integrationEventExecutor_isBoundedAndNamed() {
        assertEquals(AsyncConfig.CORE_POOL_SIZE, executor.getCorePoolSize());
        assertEquals(AsyncConfig.MAX_POOL_SIZE, executor.getMaxPoolSize());
        assertEquals(AsyncConfig.QUEUE_CAPACITY, executor.getQueueCapacity());
        assertTrue(executor.getThreadNamePrefix().startsWith(AsyncConfig.THREAD_NAME_PREFIX));
        assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class,
                executor.getThreadPoolExecutor().getRejectedExecutionHandler());
    }

    @Test
    void integrationEventExecutor_runsTasksOnNamedThreads() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();
        executor.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(threadName.get().startsWith(AsyncConfig.THREAD_NAME_PREFIX));
    }

    @Test
    void rejectedTasks_runOnCallerThread() {
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();

        AtomicReference<String> threadName = new AtomicReference<>();
        threadPoolExecutor.getRejectedExecutionHandler().rejectedExecution(
                () -> threadName.set(Thread.currentThread().getName()), threadPoolExecutor);

        assertEquals(Thread.currentThread().getName(), threadName.get());
    }

    @Test
    void integrationEventExecutor_beanNameMatchesContract() throws NoSuchMethodException {
        Method beanMethod = AsyncConfig.class.getMethod("integrationEventExecutor");
        Bean bean = beanMethod.getAnnotation(Bean.class);

        assertNotNull(bean);
        assertTrue(Arrays.asList(bean.name()).contains("integrationEventExecutor"));
    }

    @Test
    void asyncExecutor_isSameInstanceAsIntegrationEventExecutor() {
        assertSame(executor, config.getAsyncExecutor());
        assertSame(executor, config.integrationEventExecutor());
    }

    @Test
    void uncaughtExceptionHandler_isProvidedAndDoesNotThrow() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();
        Method method = AsyncConfig.class.getMethod("getAsyncExecutor");

        assertNotNull(handler);
        assertDoesNotThrow(() -> handler.handleUncaughtException(
                new RuntimeException("test"), method, new Object[0]));
    }

    @Test
    void uncaughtExceptionHandler_logsErrorWithMethodAndException() throws NoSuchMethodException {
        Logger logger = (Logger) LoggerFactory.getLogger(AsyncConfig.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();
            Method method = AsyncConfig.class.getMethod("getAsyncExecutor");

            handler.handleUncaughtException(new RuntimeException("boom"), method, new Object[0]);

            assertEquals(1, appender.list.size());
            ILoggingEvent event = appender.list.get(0);
            assertEquals(Level.ERROR, event.getLevel());
            assertTrue(event.getFormattedMessage().contains("getAsyncExecutor"));
            assertNotNull(event.getThrowableProxy());
            assertEquals("boom", event.getThrowableProxy().getMessage());
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void uncaughtExceptionHandler_handlesNullMessageAndParams() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();
        Method method = AsyncConfig.class.getMethod("getAsyncExecutor");

        assertDoesNotThrow(() -> handler.handleUncaughtException(
                new RuntimeException((String) null), method, (Object[]) null));
        assertDoesNotThrow(() -> handler.handleUncaughtException(
                new Error("severe"), method, new Object[] {"param", null}));
    }

}
