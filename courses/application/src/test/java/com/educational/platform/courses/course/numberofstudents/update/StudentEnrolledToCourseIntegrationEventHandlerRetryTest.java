package com.educational.platform.courses.course.numberofstudents.update;

import com.educational.platform.common.event.FailedIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;

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
    void doesNotRetryBusinessException() {
        // given
        final StudentEnrolledToCourseIntegrationEvent event =
                new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "username");
        doThrow(new ResourceNotFoundException("x"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any(IncreaseNumberOfStudentsCommand.class));

        // when
        try {
            sut.handleStudentEnrolledToCourseEvent(event);
        } catch (ResourceNotFoundException expected) {
            // expected to propagate without retry
        }

        // then
        verify(increaseNumberOfStudentsCommandHandler, times(1)).handle(any(IncreaseNumberOfStudentsCommand.class));
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
