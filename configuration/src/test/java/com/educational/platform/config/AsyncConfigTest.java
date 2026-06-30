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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    static class SampleAsyncBean {
        public void handleEvent(String payload) {
        }
    }
}
