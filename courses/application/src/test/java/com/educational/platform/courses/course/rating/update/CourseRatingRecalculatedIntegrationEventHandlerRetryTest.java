package com.educational.platform.courses.course.rating.update;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CourseRatingRecalculatedIntegrationEventHandlerRetryTest {

    @EnableRetry
    @Configuration
    static class RetryConfig {
    }

    @Test
    void handleCourseRatingRecalculatedEvent_commandHandlerAlwaysFails_retriedThreeTimesThenRecovered() {
        final UpdateCourseRatingCommandHandler commandHandler = mock(UpdateCourseRatingCommandHandler.class);
        doThrow(new RuntimeException("boom")).when(commandHandler).handle(any());
        final CourseRatingRecalculatedIntegrationEventHandler target = spy(new CourseRatingRecalculatedIntegrationEventHandler(commandHandler));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RetryConfig.class);
            context.registerBean(CourseRatingRecalculatedIntegrationEventHandler.class, () -> target);
            context.refresh();

            final CourseRatingRecalculatedIntegrationEventHandler sut = context.getBean(CourseRatingRecalculatedIntegrationEventHandler.class);
            final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), 4.5);

            sut.handleCourseRatingRecalculatedEvent(event);

            verify(commandHandler, times(3)).handle(any());
            verify(target).recover(any(RuntimeException.class), any(CourseRatingRecalculatedIntegrationEvent.class));
        }
    }

    @Test
    void recover_logsEventContextAndSwallowsException() {
        // given
        final UpdateCourseRatingCommandHandler commandHandler = mock(UpdateCourseRatingCommandHandler.class);
        final CourseRatingRecalculatedIntegrationEventHandler sut = new CourseRatingRecalculatedIntegrationEventHandler(commandHandler);
        final UUID courseId = UUID.randomUUID();
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(courseId, 4.5);
        final RuntimeException failure = new RuntimeException("boom");

        final Logger logger = (Logger) LoggerFactory.getLogger(CourseRatingRecalculatedIntegrationEventHandler.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            // when
            assertThatCode(() -> sut.recover(failure, event)).doesNotThrowAnyException();

            // then
            assertThat(appender.list)
                    .anySatisfy(loggingEvent -> {
                        assertThat(loggingEvent.getFormattedMessage())
                                .contains("Retries exhausted for CourseRatingRecalculatedIntegrationEvent")
                                .contains(courseId.toString())
                                .contains("4.5")
                                .contains("event is lost");
                        assertThat(loggingEvent.getThrowableProxy().getMessage()).isEqualTo("boom");
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }

}
