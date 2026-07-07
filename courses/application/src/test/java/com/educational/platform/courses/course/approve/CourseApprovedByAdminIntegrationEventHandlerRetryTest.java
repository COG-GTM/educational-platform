package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;

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

public class CourseApprovedByAdminIntegrationEventHandlerRetryTest {

    private static final ApproveCourseCommandHandler approveCourseCommandHandler = mock(ApproveCourseCommandHandler.class);

    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableResilientMethods
    static class RetryTestConfiguration {

        @Bean
        ApproveCourseCommandHandler approveCourseCommandHandler() {
            return approveCourseCommandHandler;
        }

        @Bean
        CourseApprovedByAdminIntegrationEventHandler courseApprovedByAdminIntegrationEventHandler(ApproveCourseCommandHandler handler) {
            return new CourseApprovedByAdminIntegrationEventHandler(handler);
        }

    }

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.reset(approveCourseCommandHandler);
        context = new AnnotationConfigApplicationContext(RetryTestConfiguration.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void handleCourseApprovedByAdminEvent_transientFailure_retriedUntilSuccess() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new RuntimeException("transient failure"))
                .doThrow(new RuntimeException("transient failure"))
                .doNothing()
                .when(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));
        final CourseApprovedByAdminIntegrationEventHandler sut = context.getBean(CourseApprovedByAdminIntegrationEventHandler.class);

        // when
        sut.handleCourseApprovedByAdminEvent(new CourseApprovedByAdminIntegrationEvent(uuid));

        // then
        verify(approveCourseCommandHandler, times(3)).handle(any(ApproveCourseCommand.class));
    }

    @Test
    void handleCourseApprovedByAdminEvent_persistentFailure_retriesExhaustedAndExceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doThrow(new RuntimeException("persistent failure"))
                .when(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));
        final CourseApprovedByAdminIntegrationEventHandler sut = context.getBean(CourseApprovedByAdminIntegrationEventHandler.class);

        // when // then
        assertThrows(RuntimeException.class,
                () -> sut.handleCourseApprovedByAdminEvent(new CourseApprovedByAdminIntegrationEvent(uuid)));
        verify(approveCourseCommandHandler, times(4)).handle(any(ApproveCourseCommand.class));
    }

    @Test
    void publishEventWithoutTransaction_fallbackExecution_handlerInvoked() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        doNothing().when(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));

        // when
        context.publishEvent(new CourseApprovedByAdminIntegrationEvent(uuid));

        // then
        verify(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));
    }

}
