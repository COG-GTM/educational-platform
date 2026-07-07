package com.educational.platform.administration.course.create;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

}
