package com.educational.platform.courses.course.numberofstudents.update;

import com.educational.platform.common.event.FailedEventStatus;
import com.educational.platform.common.event.FailedIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.IntegrationEventRetryHandler;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(StudentEnrolledToCourseIntegrationEventHandlerRetryTest.TestConfig.class)
class StudentEnrolledToCourseIntegrationEventHandlerRetryTest {

    @Autowired
    private StudentEnrolledToCourseIntegrationEventHandler sut;

    @Autowired
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @Autowired
    private FailedIntegrationEventRepository failedEventRepository;

    @BeforeEach
    void resetMocks() {
        reset(increaseNumberOfStudentsCommandHandler, failedEventRepository);
    }

    @Test
    void retriesTransientFailureThreeTimesThenRecovers() {
        // given
        final StudentEnrolledToCourseIntegrationEvent event =
                new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "username");
        doThrow(new OptimisticLockingFailureException("x"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any(IncreaseNumberOfStudentsCommand.class));

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        verify(increaseNumberOfStudentsCommandHandler, times(3)).handle(any(IncreaseNumberOfStudentsCommand.class));
        verify(failedEventRepository, times(1)).save(any(FailedIntegrationEvent.class));
    }

    @Test
    void retriesDataAccessExceptionThreeTimesThenRecovers() {
        // given
        final StudentEnrolledToCourseIntegrationEvent event =
                new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "username");
        doThrow(new DataAccessResourceFailureException("db down"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any(IncreaseNumberOfStudentsCommand.class));

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        verify(increaseNumberOfStudentsCommandHandler, times(3)).handle(any(IncreaseNumberOfStudentsCommand.class));
        verify(failedEventRepository, times(1)).save(any(FailedIntegrationEvent.class));
    }

    @Test
    void deadLetterRecordCapturesEventMetadataAndExhaustedRetryCount() {
        // given
        final StudentEnrolledToCourseIntegrationEvent event =
                new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "username");
        doThrow(new OptimisticLockingFailureException("stale version"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any(IncreaseNumberOfStudentsCommand.class));

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        final ArgumentCaptor<FailedIntegrationEvent> captor = ArgumentCaptor.forClass(FailedIntegrationEvent.class);
        verify(failedEventRepository).save(captor.capture());
        final FailedIntegrationEvent deadLetter = captor.getValue();
        assertThat(deadLetter.getEventClassName()).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class.getName());
        assertThat(deadLetter.getEventPayload()).isEqualTo(String.valueOf(event));
        assertThat(deadLetter.getExceptionMessage()).isEqualTo("stale version");
        assertThat(deadLetter.getRetryCount()).isEqualTo(IntegrationEventRetryHandler.MAX_ATTEMPTS);
        assertThat(deadLetter.getStatus()).isEqualTo(FailedEventStatus.FAILED);
    }

    @Test
    void doesNotRetryBusinessExceptionButStillDeadLetters() {
        // given
        final StudentEnrolledToCourseIntegrationEvent event =
                new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "username");
        doThrow(new ResourceNotFoundException("x"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any(IncreaseNumberOfStudentsCommand.class));

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then: a non-retryable (business) exception is attempted exactly once,
        // then handed to @Recover which dead-letters it (it is not propagated).
        verify(increaseNumberOfStudentsCommandHandler, times(1)).handle(any(IncreaseNumberOfStudentsCommand.class));
        verify(failedEventRepository, times(1)).save(any(FailedIntegrationEvent.class));
    }

    @Test
    void successfulHandlingNeitherRetriesNorDeadLetters() {
        // given
        final StudentEnrolledToCourseIntegrationEvent event =
                new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "username");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        verify(increaseNumberOfStudentsCommandHandler, times(1)).handle(any(IncreaseNumberOfStudentsCommand.class));
        verify(failedEventRepository, never()).save(any(FailedIntegrationEvent.class));
    }

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler() {
            return Mockito.mock(IncreaseNumberOfStudentsCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedEventRepository() {
            return Mockito.mock(FailedIntegrationEventRepository.class);
        }

        @Bean
        StudentEnrolledToCourseIntegrationEventHandler studentEnrolledToCourseIntegrationEventHandler(
                IncreaseNumberOfStudentsCommandHandler handler,
                FailedIntegrationEventRepository repository) {
            return new StudentEnrolledToCourseIntegrationEventHandler(handler, repository);
        }
    }
}
