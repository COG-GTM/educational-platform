package com.educational.platform.courses.course.numberofsudents.update;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;

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

public class StudentEnrolledToCourseIntegrationEventHandlerRetryTest {

    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableResilientMethods
    static class RetryTestConfiguration {

        @Bean
        StudentEnrolledToCourseIntegrationEventHandler studentEnrolledToCourseIntegrationEventHandler(IncreaseNumberOfStudentsCommandHandler handler) {
            return new StudentEnrolledToCourseIntegrationEventHandler(handler);
        }

    }

    @BeforeEach
    void setUp() {
        increaseNumberOfStudentsCommandHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(IncreaseNumberOfStudentsCommandHandler.class, () -> increaseNumberOfStudentsCommandHandler);
        context.register(RetryTestConfiguration.class);
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void handleStudentEnrolledToCourseEvent_transientFailure_retriedUntilSuccess() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new RuntimeException("transient failure"))
                .doThrow(new RuntimeException("transient failure"))
                .doNothing()
                .when(increaseNumberOfStudentsCommandHandler).handle(any(IncreaseNumberOfStudentsCommand.class));
        final StudentEnrolledToCourseIntegrationEventHandler sut = context.getBean(StudentEnrolledToCourseIntegrationEventHandler.class);

        // when
        sut.handleStudentEnrolledToCourseEvent(new StudentEnrolledToCourseIntegrationEvent(uuid, "student"));

        // then
        verify(increaseNumberOfStudentsCommandHandler, times(3)).handle(new IncreaseNumberOfStudentsCommand(uuid));
    }

    @Test
    void handleStudentEnrolledToCourseEvent_persistentFailure_retriesExhaustedAndExceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new RuntimeException("persistent failure"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any(IncreaseNumberOfStudentsCommand.class));
        final StudentEnrolledToCourseIntegrationEventHandler sut = context.getBean(StudentEnrolledToCourseIntegrationEventHandler.class);

        // when // then
        assertThrows(RuntimeException.class,
                () -> sut.handleStudentEnrolledToCourseEvent(new StudentEnrolledToCourseIntegrationEvent(uuid, "student")));
        verify(increaseNumberOfStudentsCommandHandler, times(4)).handle(any(IncreaseNumberOfStudentsCommand.class));
    }

    @Test
    void publishEventWithoutTransaction_fallbackExecution_handlerInvoked() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doNothing().when(increaseNumberOfStudentsCommandHandler).handle(any(IncreaseNumberOfStudentsCommand.class));

        // when
        context.publishEvent(new StudentEnrolledToCourseIntegrationEvent(uuid, "student"));

        // then
        verify(increaseNumberOfStudentsCommandHandler).handle(new IncreaseNumberOfStudentsCommand(uuid));
    }

}
