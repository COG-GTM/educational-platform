package com.educational.platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class LoggingAsyncUncaughtExceptionHandlerTest {

    private final LoggingAsyncUncaughtExceptionHandler sut = new LoggingAsyncUncaughtExceptionHandler();

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(LoggingAsyncUncaughtExceptionHandler.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    void handleUncaughtException_logsErrorWithDeclaringClassMethodNameParamsAndException() throws Exception {
        // given
        final Method method = SampleListener.class.getMethod("consume", String.class, Integer.class);
        final IllegalStateException exception = new IllegalStateException("boom");

        // when
        sut.handleUncaughtException(exception, method, "course-42", 7);

        // then
        assertThat(listAppender.list).hasSize(1);
        final ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage())
                .contains(SampleListener.class.getName())
                .contains("consume")
                .contains("[course-42, 7]");
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("boom");
    }

    @Test
    void handleUncaughtException_doesNotThrowForNoParamsAndNullExceptionMessage() throws Exception {
        // given
        final Method method = SampleListener.class.getMethod("noArgs");

        // when / then
        assertThatCode(() -> sut.handleUncaughtException(new RuntimeException((String) null), method))
                .doesNotThrowAnyException();
        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getFormattedMessage()).contains("[]");
    }

    @Test
    void isComponent_soItIsDiscoveredByComponentScan() {
        // then
        assertThat(LoggingAsyncUncaughtExceptionHandler.class.isAnnotationPresent(Component.class)).isTrue();
    }

    static class SampleListener {

        public void consume(String courseId, Integer attempt) {
        }

        public void noArgs() {
        }
    }
}
