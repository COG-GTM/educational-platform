package com.educational.platform.async;

import com.educational.platform.common.async.AsyncConfiguration;

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

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigurationTest {

    private AsyncConfiguration asyncConfiguration;
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        asyncConfiguration = new AsyncConfiguration();
        executor = asyncConfiguration.integrationEventExecutor();
        executor.initialize();
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void integrationEventExecutor_hasExpectedConfiguration() {
        assertEquals(4, executor.getCorePoolSize());
        assertEquals(8, executor.getMaxPoolSize());
        assertEquals("integration-event-", executor.getThreadNamePrefix());
        assertEquals(100, executor.getThreadPoolExecutor().getQueue().remainingCapacity());
        assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class, executor.getThreadPoolExecutor().getRejectedExecutionHandler());
        assertNotNull(asyncConfiguration.getAsyncUncaughtExceptionHandler());
    }

    @Test
    void integrationEventExecutor_runsTaskOnPrefixedThread() throws InterruptedException {
        // given
        final AtomicReference<String> threadName = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        // when
        executor.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        // then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertThat(threadName.get()).startsWith("integration-event-");
    }

    @Test
    void asyncUncaughtExceptionHandler_logsErrorAndDoesNotThrow() throws NoSuchMethodException {
        // given
        final AsyncUncaughtExceptionHandler handler = asyncConfiguration.getAsyncUncaughtExceptionHandler();
        final Method method = Object.class.getDeclaredMethod("toString");
        final Logger logger = (Logger) LoggerFactory.getLogger(AsyncConfiguration.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            // when
            assertDoesNotThrow(() -> handler.handleUncaughtException(new IllegalStateException("boom"), method, "param"));
            assertDoesNotThrow(() -> handler.handleUncaughtException(new IllegalStateException(), method));

            // then
            assertThat(appender.list)
                    .hasSize(2)
                    .allSatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                        assertThat(event.getFormattedMessage()).contains("Uncaught async exception").contains("toString");
                        assertThat(event.getThrowableProxy().getClassName()).isEqualTo(IllegalStateException.class.getName());
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void getAsyncExecutor_returnsExecutorWithIntegrationEventConfiguration() {
        final Object asyncExecutor = asyncConfiguration.getAsyncExecutor();

        assertInstanceOf(ThreadPoolTaskExecutor.class, asyncExecutor);
        final ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) asyncExecutor;
        assertEquals(4, taskExecutor.getCorePoolSize());
        assertEquals(8, taskExecutor.getMaxPoolSize());
        assertEquals("integration-event-", taskExecutor.getThreadNamePrefix());
    }
}
