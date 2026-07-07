package com.educational.platform.courses.teacher.create;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

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

public class UserCreatedIntegrationEventHandlerRetryTest {

    @EnableRetry
    @Configuration
    static class RetryConfig {
    }

    @Test
    void handleUserCreatedEvent_commandHandlerAlwaysFails_retriedThreeTimesThenRecovered() {
        final CreateTeacherCommandHandler commandHandler = mock(CreateTeacherCommandHandler.class);
        doThrow(new RuntimeException("boom")).when(commandHandler).handle(any());
        final UserCreatedIntegrationEventHandler target = spy(new UserCreatedIntegrationEventHandler(commandHandler));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RetryConfig.class);
            context.registerBean(UserCreatedIntegrationEventHandler.class, () -> target);
            context.refresh();

            final UserCreatedIntegrationEventHandler sut = context.getBean(UserCreatedIntegrationEventHandler.class);
            final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

            sut.handleUserCreatedEvent(event);

            verify(commandHandler, times(3)).handle(any());
            verify(target).recover(any(RuntimeException.class), any(UserCreatedIntegrationEvent.class));
        }
    }

    @Test
    void handleUserCreatedEvent_commandHandlerFailsTwiceThenSucceeds_notRecovered() {
        final CreateTeacherCommandHandler commandHandler = mock(CreateTeacherCommandHandler.class);
        doThrow(new RuntimeException("boom"))
                .doThrow(new RuntimeException("boom"))
                .doNothing()
                .when(commandHandler).handle(any());
        final UserCreatedIntegrationEventHandler target = spy(new UserCreatedIntegrationEventHandler(commandHandler));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RetryConfig.class);
            context.registerBean(UserCreatedIntegrationEventHandler.class, () -> target);
            context.refresh();

            final UserCreatedIntegrationEventHandler sut = context.getBean(UserCreatedIntegrationEventHandler.class);
            final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

            sut.handleUserCreatedEvent(event);

            verify(commandHandler, times(3)).handle(any());
            verify(target, never()).recover(any(), any());
        }
    }

    @Test
    void recover_logsEventContextAndSwallowsException() {
        // given
        final CreateTeacherCommandHandler commandHandler = mock(CreateTeacherCommandHandler.class);
        final UserCreatedIntegrationEventHandler sut = new UserCreatedIntegrationEventHandler(commandHandler);
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");
        final RuntimeException failure = new RuntimeException("boom");

        final Logger logger = (Logger) LoggerFactory.getLogger(UserCreatedIntegrationEventHandler.class);
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
                                .contains("Retries exhausted for UserCreatedIntegrationEvent")
                                .contains("username")
                                .contains("event is lost");
                        assertThat(loggingEvent.getThrowableProxy().getMessage()).isEqualTo("boom");
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }

}
