package com.educational.platform.courses.course.numberofstudents.update;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;

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

class StudentEnrolledToCourseIntegrationEventHandlerRetryTest {

    private AnnotationConfigApplicationContext context;
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;
    private StudentEnrolledToCourseIntegrationEventHandler sut;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        increaseNumberOfStudentsCommandHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(IncreaseNumberOfStudentsCommandHandler.class, () -> increaseNumberOfStudentsCommandHandler);
        context.register(RetryConfiguration.class);
        context.refresh();
        sut = context.getBean(StudentEnrolledToCourseIntegrationEventHandler.class);

        logger = (Logger) LoggerFactory.getLogger(StudentEnrolledToCourseIntegrationEventHandler.class);
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
    void handleStudentEnrolledToCourseEvent_alwaysFails_retriedThreeTimesThenRecoverLogsAndSwallows() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new IllegalStateException("persistent failure"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any(IncreaseNumberOfStudentsCommand.class));

        // when
        assertThatCode(() -> sut.handleStudentEnrolledToCourseEvent(event)).doesNotThrowAnyException();

        // then
        verify(increaseNumberOfStudentsCommandHandler, times(3)).handle(any(IncreaseNumberOfStudentsCommand.class));
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
    void handleStudentEnrolledToCourseEvent_failsTwiceThenSucceeds_notRecovered() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new IllegalStateException("transient failure"))
                .doThrow(new IllegalStateException("transient failure"))
                .doNothing()
                .when(increaseNumberOfStudentsCommandHandler).handle(any(IncreaseNumberOfStudentsCommand.class));

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        verify(increaseNumberOfStudentsCommandHandler, times(3)).handle(any(IncreaseNumberOfStudentsCommand.class));
        assertThat(appender.list).noneMatch(loggingEvent -> loggingEvent.getLevel() == Level.ERROR);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_succeedsFirstAttempt_invokedOnce() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doNothing().when(increaseNumberOfStudentsCommandHandler).handle(any(IncreaseNumberOfStudentsCommand.class));

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        verify(increaseNumberOfStudentsCommandHandler, times(1)).handle(any(IncreaseNumberOfStudentsCommand.class));
        assertThat(appender.list).noneMatch(loggingEvent -> loggingEvent.getLevel() == Level.ERROR);
    }

    @Configuration
    @EnableRetry
    static class RetryConfiguration {

        @Bean
        StudentEnrolledToCourseIntegrationEventHandler studentEnrolledToCourseIntegrationEventHandler(IncreaseNumberOfStudentsCommandHandler handler) {
            return new StudentEnrolledToCourseIntegrationEventHandler(handler);
        }
    }
}
