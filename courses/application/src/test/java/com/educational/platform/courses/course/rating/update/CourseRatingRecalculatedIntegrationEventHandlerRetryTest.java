package com.educational.platform.courses.course.rating.update;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CourseRatingRecalculatedIntegrationEventHandlerRetryTest {

    private AnnotationConfigApplicationContext context;
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;
    private CourseRatingRecalculatedIntegrationEventHandler sut;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        updateCourseRatingCommandHandler = mock(UpdateCourseRatingCommandHandler.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(UpdateCourseRatingCommandHandler.class, () -> updateCourseRatingCommandHandler);
        context.register(RetryConfiguration.class);
        context.refresh();
        sut = context.getBean(CourseRatingRecalculatedIntegrationEventHandler.class);

        logger = (Logger) LoggerFactory.getLogger(CourseRatingRecalculatedIntegrationEventHandler.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        context.close();
    }

    @Test
    void handleCourseRatingRecalculatedEvent_alwaysFails_retriedThreeTimesThenRecoverLogsAndSwallows() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new IllegalStateException("persistent failure"))
                .when(updateCourseRatingCommandHandler).handle(any(UpdateCourseRatingCommand.class));

        // when
        assertThatCode(() -> sut.handleCourseRatingRecalculatedEvent(event)).doesNotThrowAnyException();

        // then
        verify(updateCourseRatingCommandHandler, times(3)).handle(any(UpdateCourseRatingCommand.class));
        assertThat(appender.list)
                .anySatisfy(loggingEvent -> {
                    assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
                    assertThat(loggingEvent.getFormattedMessage())
                            .contains("Integration event ultimately failed after retries")
                            .contains(uuid.toString());
                    assertThat(loggingEvent.getThrowableProxy().getClassName()).isEqualTo(IllegalStateException.class.getName());
                });
    }

    @Test
    void handleCourseRatingRecalculatedEvent_failsTwiceThenSucceeds_notRecovered() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new IllegalStateException("transient failure"))
                .doThrow(new IllegalStateException("transient failure"))
                .doNothing()
                .when(updateCourseRatingCommandHandler).handle(any(UpdateCourseRatingCommand.class));

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        verify(updateCourseRatingCommandHandler, times(3)).handle(any(UpdateCourseRatingCommand.class));
        assertThat(appender.list).noneMatch(loggingEvent -> loggingEvent.getLevel() == Level.ERROR);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_succeedsFirstAttempt_invokedOnce() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doNothing().when(updateCourseRatingCommandHandler).handle(any(UpdateCourseRatingCommand.class));

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        verify(updateCourseRatingCommandHandler, times(1)).handle(any(UpdateCourseRatingCommand.class));
        assertThat(appender.list).noneMatch(loggingEvent -> loggingEvent.getLevel() == Level.ERROR);
    }

    @Configuration
    @EnableRetry
    static class RetryConfiguration {

        @Bean
        CourseRatingRecalculatedIntegrationEventHandler courseRatingRecalculatedIntegrationEventHandler(UpdateCourseRatingCommandHandler handler) {
            return new CourseRatingRecalculatedIntegrationEventHandler(handler);
        }
    }
}
