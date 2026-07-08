package com.educational.platform.administration.course.create;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

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

class SendCourseToApproveIntegrationEventHandlerRetryTest {

    private AnnotationConfigApplicationContext context;
    private CreateCourseProposalCommandHandler createCourseProposalCommandHandler;
    private SendCourseToApproveIntegrationEventHandler sut;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        createCourseProposalCommandHandler = mock(CreateCourseProposalCommandHandler.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(CreateCourseProposalCommandHandler.class, () -> createCourseProposalCommandHandler);
        context.register(RetryConfiguration.class);
        context.refresh();
        sut = context.getBean(SendCourseToApproveIntegrationEventHandler.class);

        logger = (Logger) LoggerFactory.getLogger(SendCourseToApproveIntegrationEventHandler.class);
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
    void handleSendCourseToApproveEvent_alwaysFails_retriedThreeTimesThenRecoverLogsAndSwallows() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new IllegalStateException("persistent failure"))
                .when(createCourseProposalCommandHandler).handle(any(CreateCourseProposalCommand.class));

        // when
        assertThatCode(() -> sut.handleSendCourseToApproveEvent(event)).doesNotThrowAnyException();

        // then
        verify(createCourseProposalCommandHandler, times(3)).handle(any(CreateCourseProposalCommand.class));
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
    void handleSendCourseToApproveEvent_failsTwiceThenSucceeds_notRecovered() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new IllegalStateException("transient failure"))
                .doThrow(new IllegalStateException("transient failure"))
                .doNothing()
                .when(createCourseProposalCommandHandler).handle(any(CreateCourseProposalCommand.class));

        // when
        sut.handleSendCourseToApproveEvent(event);

        // then
        verify(createCourseProposalCommandHandler, times(3)).handle(any(CreateCourseProposalCommand.class));
        assertThat(appender.list).noneMatch(loggingEvent -> loggingEvent.getLevel() == Level.ERROR);
    }

    @Test
    void handleSendCourseToApproveEvent_succeedsFirstAttempt_invokedOnce() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doNothing().when(createCourseProposalCommandHandler).handle(any(CreateCourseProposalCommand.class));

        // when
        sut.handleSendCourseToApproveEvent(event);

        // then
        verify(createCourseProposalCommandHandler, times(1)).handle(any(CreateCourseProposalCommand.class));
        assertThat(appender.list).noneMatch(loggingEvent -> loggingEvent.getLevel() == Level.ERROR);
    }

    @Configuration
    @EnableRetry
    static class RetryConfiguration {

        @Bean
        SendCourseToApproveIntegrationEventHandler sendCourseToApproveIntegrationEventHandler(CreateCourseProposalCommandHandler handler) {
            return new SendCourseToApproveIntegrationEventHandler(handler);
        }
    }
}
