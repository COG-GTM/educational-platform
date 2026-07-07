package com.educational.platform.administration.course.create;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SendCourseToApproveIntegrationEventHandlerRetryTest {

    private CreateCourseProposalCommandHandler createCourseProposalCommandHandler;

    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableResilientMethods
    static class RetryTestConfiguration {

        @Bean
        SendCourseToApproveIntegrationEventHandler sendCourseToApproveIntegrationEventHandler(CreateCourseProposalCommandHandler handler) {
            return new SendCourseToApproveIntegrationEventHandler(handler);
        }

    }

    @BeforeEach
    void setUp() {
        createCourseProposalCommandHandler = mock(CreateCourseProposalCommandHandler.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(CreateCourseProposalCommandHandler.class, () -> createCourseProposalCommandHandler);
        context.register(RetryTestConfiguration.class);
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void handleSendCourseToApproveEvent_transientFailure_retriedUntilSuccess() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new RuntimeException("transient failure"))
                .doThrow(new RuntimeException("transient failure"))
                .doNothing()
                .when(createCourseProposalCommandHandler).handle(any(CreateCourseProposalCommand.class));
        final SendCourseToApproveIntegrationEventHandler sut = context.getBean(SendCourseToApproveIntegrationEventHandler.class);

        // when
        sut.handleSendCourseToApproveEvent(new SendCourseToApproveIntegrationEvent(uuid));

        // then
        verify(createCourseProposalCommandHandler, times(3)).handle(any(CreateCourseProposalCommand.class));
    }

    @Test
    void handleSendCourseToApproveEvent_persistentFailure_retriesExhaustedAndExceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new RuntimeException("persistent failure"))
                .when(createCourseProposalCommandHandler).handle(any(CreateCourseProposalCommand.class));
        final SendCourseToApproveIntegrationEventHandler sut = context.getBean(SendCourseToApproveIntegrationEventHandler.class);

        // when // then
        assertThrows(RuntimeException.class,
                () -> sut.handleSendCourseToApproveEvent(new SendCourseToApproveIntegrationEvent(uuid)));
        verify(createCourseProposalCommandHandler, times(4)).handle(any(CreateCourseProposalCommand.class));
    }

    @Test
    void publishEventWithoutTransaction_fallbackExecution_handlerInvoked() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doNothing().when(createCourseProposalCommandHandler).handle(any(CreateCourseProposalCommand.class));

        // when
        context.publishEvent(new SendCourseToApproveIntegrationEvent(uuid));

        // then
        verify(createCourseProposalCommandHandler).handle(any(CreateCourseProposalCommand.class));
    }

}
