package com.educational.platform.courses.course.numberofstudents.update;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;

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

public class StudentEnrolledToCourseIntegrationEventHandlerRetryTest {

    @EnableRetry
    @Configuration
    static class RetryConfig {
    }

    @Test
    void handleStudentEnrolledToCourseEvent_commandHandlerAlwaysFails_retriedThreeTimesThenRecovered() {
        final IncreaseNumberOfStudentsCommandHandler commandHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        doThrow(new RuntimeException("boom")).when(commandHandler).handle(any());
        final StudentEnrolledToCourseIntegrationEventHandler target = spy(new StudentEnrolledToCourseIntegrationEventHandler(commandHandler));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RetryConfig.class);
            context.registerBean(StudentEnrolledToCourseIntegrationEventHandler.class, () -> target);
            context.refresh();

            final StudentEnrolledToCourseIntegrationEventHandler sut = context.getBean(StudentEnrolledToCourseIntegrationEventHandler.class);
            final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "username");

            sut.handleStudentEnrolledToCourseEvent(event);

            verify(commandHandler, times(3)).handle(any());
            verify(target).recover(any(RuntimeException.class), any(StudentEnrolledToCourseIntegrationEvent.class));
        }
    }

    @Test
    void recover_logsEventContextAndSwallowsException() {
        // given
        final IncreaseNumberOfStudentsCommandHandler commandHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        final StudentEnrolledToCourseIntegrationEventHandler sut = new StudentEnrolledToCourseIntegrationEventHandler(commandHandler);
        final UUID courseId = UUID.randomUUID();
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "username");
        final RuntimeException failure = new RuntimeException("boom");

        final Logger logger = (Logger) LoggerFactory.getLogger(StudentEnrolledToCourseIntegrationEventHandler.class);
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
                                .contains("Retries exhausted for StudentEnrolledToCourseIntegrationEvent")
                                .contains(courseId.toString())
                                .contains("username")
                                .contains("event is lost");
                        assertThat(loggingEvent.getThrowableProxy().getMessage()).isEqualTo("boom");
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }

}
