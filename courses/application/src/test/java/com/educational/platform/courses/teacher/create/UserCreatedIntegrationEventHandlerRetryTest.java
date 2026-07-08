package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class UserCreatedIntegrationEventHandlerRetryTest {

    private AnnotationConfigApplicationContext context;
    private CreateTeacherCommandHandler createTeacherCommandHandler;
    private UserCreatedIntegrationEventHandler sut;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        createTeacherCommandHandler = mock(CreateTeacherCommandHandler.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(CreateTeacherCommandHandler.class, () -> createTeacherCommandHandler);
        context.register(RetryConfiguration.class);
        context.refresh();
        sut = context.getBean(UserCreatedIntegrationEventHandler.class);

        logger = (Logger) LoggerFactory.getLogger(UserCreatedIntegrationEventHandler.class);
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
    void handleUserCreatedEvent_alwaysFails_retriedThreeTimesThenRecoverLogsAndSwallows() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "email@gmail.com");
        doThrow(new IllegalStateException("persistent failure"))
                .when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when
        assertThatCode(() -> sut.handleUserCreatedEvent(event)).doesNotThrowAnyException();

        // then
        verify(createTeacherCommandHandler, times(3)).handle(any(CreateTeacherCommand.class));
        assertThat(appender.list)
                .anySatisfy(loggingEvent -> {
                    assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
                    assertThat(loggingEvent.getFormattedMessage())
                            .contains("Integration event ultimately failed after retries")
                            .contains("username");
                    assertThat(loggingEvent.getThrowableProxy().getClassName()).isEqualTo(IllegalStateException.class.getName());
                });
    }

    @Test
    void handleUserCreatedEvent_failsTwiceThenSucceeds_notRecovered() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "email@gmail.com");
        doThrow(new IllegalStateException("transient failure"))
                .doThrow(new IllegalStateException("transient failure"))
                .doNothing()
                .when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when
        sut.handleUserCreatedEvent(event);

        // then
        verify(createTeacherCommandHandler, times(3)).handle(any(CreateTeacherCommand.class));
        assertThat(appender.list).noneMatch(loggingEvent -> loggingEvent.getLevel() == Level.ERROR);
    }

    @Test
    void handleUserCreatedEvent_succeedsFirstAttempt_invokedOnce() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "email@gmail.com");
        doNothing().when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when
        sut.handleUserCreatedEvent(event);

        // then
        verify(createTeacherCommandHandler, times(1)).handle(any(CreateTeacherCommand.class));
        assertThat(appender.list).noneMatch(loggingEvent -> loggingEvent.getLevel() == Level.ERROR);
    }

    @Configuration
    @EnableRetry
    static class RetryConfiguration {

        @Bean
        UserCreatedIntegrationEventHandler userCreatedIntegrationEventHandler(CreateTeacherCommandHandler handler) {
            return new UserCreatedIntegrationEventHandler(handler);
        }
    }
}
