package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.common.event.FailedEventStatus;
import com.educational.platform.common.event.FailedIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.IntegrationEventRetryHandler;
import com.educational.platform.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

    @Test
    void businessFailure_isNotRetriedButStillDeadLettered() {
        // given a non-retryable business exception
        final CourseApprovedByAdminIntegrationEvent event =
                new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());
        doThrow(new ResourceNotFoundException("x"))
                .when(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then it is attempted once (no retry) but the failure is still routed to the dead-letter store
        verify(approveCourseCommandHandler, times(1)).handle(any(ApproveCourseCommand.class));
        verify(failedEventRepository, times(1)).save(any());
    }

    @Test
    void transientDataAccessFailure_isRetried() {
        // given a generic DataAccessException (the other declared retryFor type)
        final CourseApprovedByAdminIntegrationEvent event =
                new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());
        doThrow(new RecoverableDataAccessException("db down"))
                .when(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then it is retried up to maxAttempts and then dead-lettered
        verify(approveCourseCommandHandler, times(IntegrationEventRetryHandler.MAX_ATTEMPTS))
                .handle(any(ApproveCourseCommand.class));
        verify(failedEventRepository, times(1)).save(any());
    }

    @Test
    void transientFailureRecoveringBeforeMaxAttempts_doesNotDeadLetter() {
        // given a transient failure that succeeds on the second attempt
        final CourseApprovedByAdminIntegrationEvent event =
                new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());
        doThrow(new OptimisticLockingFailureException("transient"))
                .doNothing()
                .when(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then it retries once, succeeds, and nothing is dead-lettered
        verify(approveCourseCommandHandler, times(2)).handle(any(ApproveCourseCommand.class));
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void deadLetterRecord_capturesEventDetailsAndExhaustedRetryCount() {
        // given
        final CourseApprovedByAdminIntegrationEvent event =
                new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());
        doThrow(new OptimisticLockingFailureException("boom"))
                .when(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));

        // when
        handler.handleCourseApprovedByAdminEvent(event);

        // then the persisted dead-letter record reflects the failed event and the exhausted retry count
        final ArgumentCaptor<FailedIntegrationEvent> captor = ArgumentCaptor.forClass(FailedIntegrationEvent.class);
        verify(failedEventRepository, times(1)).save(captor.capture());
        final FailedIntegrationEvent record = captor.getValue();
        assertThat(record.getEventClassName()).isEqualTo(CourseApprovedByAdminIntegrationEvent.class.getName());
        assertThat(record.getEventPayload()).isEqualTo(String.valueOf(event));
        assertThat(record.getExceptionMessage()).isEqualTo("boom");
        assertThat(record.getRetryCount()).isEqualTo(IntegrationEventRetryHandler.MAX_ATTEMPTS);
        assertThat(record.getStatus()).isEqualTo(FailedEventStatus.FAILED);
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
