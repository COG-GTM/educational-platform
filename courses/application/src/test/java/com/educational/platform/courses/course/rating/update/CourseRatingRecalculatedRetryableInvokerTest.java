package com.educational.platform.courses.course.rating.update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.educational.platform.common.retry.IntegrationEventRetryPolicy;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@SpringJUnitConfig(CourseRatingRecalculatedRetryableInvokerTest.TestConfig.class)
class CourseRatingRecalculatedRetryableInvokerTest {

    @MockitoBean
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @Autowired
    private CourseRatingRecalculatedRetryableInvoker sut;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(CourseRatingRecalculatedRetryableInvoker.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    void invoke_transientFailureThenSuccess_retriesAndReturns() {
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), 4.5d);
        final AtomicInteger attempts = new AtomicInteger();
        org.mockito.Mockito.doAnswer(invocation -> {
            if (attempts.getAndIncrement() < IntegrationEventRetryPolicy.MAX_ATTEMPTS - 1) {
                throw new RuntimeException("transient");
            }
            return null;
        }).when(updateCourseRatingCommandHandler).handle(any());

        assertThatCode(() -> sut.invoke(event)).doesNotThrowAnyException();
        verify(updateCourseRatingCommandHandler, times(IntegrationEventRetryPolicy.MAX_ATTEMPTS)).handle(any());
        assertThat(listAppender.list).isEmpty();
    }

    @Test
    void invoke_exhaustion_logsErrorAndReturns() {
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), 4.5d);
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(updateCourseRatingCommandHandler).handle(any());

        assertThatCode(() -> sut.invoke(event)).doesNotThrowAnyException();
        verify(updateCourseRatingCommandHandler, times(IntegrationEventRetryPolicy.MAX_ATTEMPTS)).handle(any());

        assertThat(listAppender.list).hasSize(1);
        final ILoggingEvent loggingEvent = listAppender.list.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(loggingEvent.getFormattedMessage()).contains("Integration event 'CourseRatingRecalculatedIntegrationEvent' exhausted retries");
        assertThat(loggingEvent.getFormattedMessage()).contains(event.toString());
        assertThat(loggingEvent.getThrowableProxy().getMessage()).isEqualTo("boom");
    }

    @Configuration
    @EnableRetry
    @Import(CourseRatingRecalculatedRetryableInvoker.class)
    static class TestConfig {
    }
}
