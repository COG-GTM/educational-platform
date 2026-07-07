package com.educational.platform.courses.course.numberofstudents.update;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;

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

}
