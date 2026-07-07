package com.educational.platform.courses.course.approve;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CourseApprovedByAdminIntegrationEventHandlerRetryTest {

    @EnableRetry
    @Configuration
    static class RetryConfig {
    }

    @Test
    void handleCourseApprovedByAdminEvent_commandHandlerAlwaysFails_retriedThreeTimesThenRecovered() {
        final ApproveCourseCommandHandler commandHandler = mock(ApproveCourseCommandHandler.class);
        doThrow(new RuntimeException("boom")).when(commandHandler).handle(any());
        final CourseApprovedByAdminIntegrationEventHandler target = spy(new CourseApprovedByAdminIntegrationEventHandler(commandHandler));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RetryConfig.class);
            context.registerBean(CourseApprovedByAdminIntegrationEventHandler.class, () -> target);
            context.refresh();

            final CourseApprovedByAdminIntegrationEventHandler sut = context.getBean(CourseApprovedByAdminIntegrationEventHandler.class);
            final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());

            sut.handleCourseApprovedByAdminEvent(event);

            verify(commandHandler, times(3)).handle(any());
            verify(target).recover(any(RuntimeException.class), any(CourseApprovedByAdminIntegrationEvent.class));
        }
    }

    @Test
    void handleCourseApprovedByAdminEvent_commandHandlerFailsTwiceThenSucceeds_notRecovered() {
        final ApproveCourseCommandHandler commandHandler = mock(ApproveCourseCommandHandler.class);
        doThrow(new RuntimeException("boom"))
                .doThrow(new RuntimeException("boom"))
                .doNothing()
                .when(commandHandler).handle(any());
        final CourseApprovedByAdminIntegrationEventHandler target = spy(new CourseApprovedByAdminIntegrationEventHandler(commandHandler));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RetryConfig.class);
            context.registerBean(CourseApprovedByAdminIntegrationEventHandler.class, () -> target);
            context.refresh();

            final CourseApprovedByAdminIntegrationEventHandler sut = context.getBean(CourseApprovedByAdminIntegrationEventHandler.class);
            final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());

            sut.handleCourseApprovedByAdminEvent(event);

            verify(commandHandler, times(3)).handle(any());
            verify(target, never()).recover(any(), any());
        }
    }

    @Test
    void recover_logsEventContextAndSwallowsException() {
        // given
        final ApproveCourseCommandHandler commandHandler = mock(ApproveCourseCommandHandler.class);
        final CourseApprovedByAdminIntegrationEventHandler sut = new CourseApprovedByAdminIntegrationEventHandler(commandHandler);
        final UUID courseId = UUID.randomUUID();
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(courseId);
        final RuntimeException failure = new RuntimeException("boom");

        final Logger logger = (Logger) LoggerFactory.getLogger(CourseApprovedByAdminIntegrationEventHandler.class);
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
                                .contains("Retries exhausted for CourseApprovedByAdminIntegrationEvent")
                                .contains(courseId.toString())
                                .contains("event is lost");
                        assertThat(loggingEvent.getThrowableProxy().getMessage()).isEqualTo("boom");
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }

}
