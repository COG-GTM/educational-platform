package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Verifies the declarative {@code @Retryable}/{@code @Recover} behaviour of
 * {@link CourseApprovedByAdminIntegrationEventHandler} inside a Spring context with retry enabled.
 */
@SpringJUnitConfig(CourseApprovedByAdminIntegrationEventHandlerRetryTest.TestConfig.class)
class CourseApprovedByAdminIntegrationEventHandlerRetryTest {

    @Autowired
    private CourseApprovedByAdminIntegrationEventHandler handler;

    @Autowired
    private ApproveCourseCommandHandler approveCourseCommandHandler;

    @Autowired
    private FailedIntegrationEventRepository failedEventRepository;

    @BeforeEach
    void resetMocks() {
        reset(approveCourseCommandHandler, failedEventRepository);
    }

    @Test
    void transientFailure_retriesThreeTimesThenRecovers() {
        // given
        final CourseApprovedByAdminIntegrationEvent event =
                new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());
        doThrow(new OptimisticLockingFailureException("x"))
                .when(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then
        verify(approveCourseCommandHandler, times(3)).handle(any(ApproveCourseCommand.class));
        verify(failedEventRepository, times(1)).save(any());
    }

    @Test
    void businessFailure_isNotRetried() {
        // given
        final CourseApprovedByAdminIntegrationEvent event =
                new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());
        doThrow(new ResourceNotFoundException("x"))
                .when(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then
        verify(approveCourseCommandHandler, times(1)).handle(any(ApproveCourseCommand.class));
    }

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        ApproveCourseCommandHandler approveCourseCommandHandler() {
            return Mockito.mock(ApproveCourseCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedEventRepository() {
            return Mockito.mock(FailedIntegrationEventRepository.class);
        }

        @Bean
        CourseApprovedByAdminIntegrationEventHandler courseApprovedByAdminIntegrationEventHandler(
                ApproveCourseCommandHandler approveCourseCommandHandler,
                FailedIntegrationEventRepository failedEventRepository) {
            return new CourseApprovedByAdminIntegrationEventHandler(
                    approveCourseCommandHandler, failedEventRepository);
        }
    }
}
