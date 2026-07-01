package com.educational.platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.educational.platform.config.AsyncConfig.LoggingAsyncUncaughtExceptionHandler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class AsyncConfigTest {

    private LoggingAsyncUncaughtExceptionHandler handler;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
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
    void logsErrorWithMethodNameAndDoesNotThrow() throws NoSuchMethodException {
        final Method method = SampleAsyncTarget.class.getDeclaredMethod("doWork", String.class);
        final RuntimeException exception = new IllegalStateException("boom");

        assertThatCode(() -> handler.handleUncaughtException(exception, method, "arg-value"))
                .doesNotThrowAnyException();

        final List<ILoggingEvent> events = appender.list;
        assertThat(events).hasSize(1);
        final ILoggingEvent event = events.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).contains("doWork");
        assertThat(event.getThrowableProxy()).isNotNull();
    }

    @SuppressWarnings("unused")
    private static final class SampleAsyncTarget {
        void doWork(String value) {
            // no-op target used only to obtain a Method reference
        }
    }
}
