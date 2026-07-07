package com.educational.platform.administration.course.create;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

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

public class SendCourseToApproveIntegrationEventHandlerRetryTest {

    @EnableRetry
    @Configuration
    static class RetryConfig {
    }

    @Test
    void handleSendCourseToApproveEvent_commandHandlerAlwaysFails_retriedThreeTimesThenRecovered() {
        final CreateCourseProposalCommandHandler commandHandler = mock(CreateCourseProposalCommandHandler.class);
        doThrow(new RuntimeException("boom")).when(commandHandler).handle(any());
        final SendCourseToApproveIntegrationEventHandler target = spy(new SendCourseToApproveIntegrationEventHandler(commandHandler));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RetryConfig.class);
            context.registerBean(SendCourseToApproveIntegrationEventHandler.class, () -> target);
            context.refresh();

            final SendCourseToApproveIntegrationEventHandler sut = context.getBean(SendCourseToApproveIntegrationEventHandler.class);
            final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());

            sut.handleSendCourseToApproveEvent(event);

            verify(commandHandler, times(3)).handle(any());
            verify(target).recover(any(RuntimeException.class), any(SendCourseToApproveIntegrationEvent.class));
        }
    }

    @Test
    void handleSendCourseToApproveEvent_commandHandlerFailsTwiceThenSucceeds_notRecovered() {
        final CreateCourseProposalCommandHandler commandHandler = mock(CreateCourseProposalCommandHandler.class);
        doThrow(new RuntimeException("boom"))
                .doThrow(new RuntimeException("boom"))
                .doNothing()
                .when(commandHandler).handle(any());
        final SendCourseToApproveIntegrationEventHandler target = spy(new SendCourseToApproveIntegrationEventHandler(commandHandler));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RetryConfig.class);
            context.registerBean(SendCourseToApproveIntegrationEventHandler.class, () -> target);
            context.refresh();

            final SendCourseToApproveIntegrationEventHandler sut = context.getBean(SendCourseToApproveIntegrationEventHandler.class);
            final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());

            sut.handleSendCourseToApproveEvent(event);

            verify(commandHandler, times(3)).handle(any());
            verify(target, never()).recover(any(), any());
        }
    }

    @Test
    void handleSendCourseToApproveEvent_commandHandlerSucceedsFirstAttempt_calledOnceAndNotRecovered() {
        final CreateCourseProposalCommandHandler commandHandler = mock(CreateCourseProposalCommandHandler.class);
        doNothing().when(commandHandler).handle(any());
        final SendCourseToApproveIntegrationEventHandler target = spy(new SendCourseToApproveIntegrationEventHandler(commandHandler));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RetryConfig.class);
            context.registerBean(SendCourseToApproveIntegrationEventHandler.class, () -> target);
            context.refresh();

            final SendCourseToApproveIntegrationEventHandler sut = context.getBean(SendCourseToApproveIntegrationEventHandler.class);
            final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());

            sut.handleSendCourseToApproveEvent(event);

            verify(commandHandler, times(1)).handle(any());
            verify(target, never()).recover(any(), any());
        }
    }

    @Test
    void recover_logsEventContextAndSwallowsException() {
        // given
        final CreateCourseProposalCommandHandler commandHandler = mock(CreateCourseProposalCommandHandler.class);
        final SendCourseToApproveIntegrationEventHandler sut = new SendCourseToApproveIntegrationEventHandler(commandHandler);
        final UUID courseId = UUID.randomUUID();
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(courseId);
        final RuntimeException failure = new RuntimeException("boom");

        final Logger logger = (Logger) LoggerFactory.getLogger(SendCourseToApproveIntegrationEventHandler.class);
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
                                .contains("Retries exhausted for SendCourseToApproveIntegrationEvent")
                                .contains(courseId.toString())
                                .contains("event is lost");
                        assertThat(loggingEvent.getThrowableProxy().getMessage()).isEqualTo("boom");
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }

}
