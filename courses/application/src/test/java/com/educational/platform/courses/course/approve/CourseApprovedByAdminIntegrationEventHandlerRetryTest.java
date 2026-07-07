package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;

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

}
