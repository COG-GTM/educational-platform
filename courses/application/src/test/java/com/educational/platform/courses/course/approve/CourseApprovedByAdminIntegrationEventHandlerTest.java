package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CourseApprovedByAdminIntegrationEventHandlerTest {

    @Mock
    private ApproveCourseCommandHandler approveCourseCommandHandler;

    @Mock
    private FailedIntegrationEventRepository failedEventRepository;

    @InjectMocks
    private CourseApprovedByAdminIntegrationEventHandler sut;

    @Test
    void handleCourseApprovedByAdminEvent_approveCourseCommandExecuted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);

        // when
        sut.handleCourseApprovedByAdminEvent(event);

        // then
        final ArgumentCaptor<ApproveCourseCommand> argument = ArgumentCaptor.forClass(ApproveCourseCommand.class);
        verify(approveCourseCommandHandler).handle(argument.capture());
        final ApproveCourseCommand approveCourseCommand = argument.getValue();
        assertThat(approveCourseCommand)
                .hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void handleCourseApprovedByAdminEvent_transientException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doThrow(new OptimisticLockingFailureException("DB connection lost"))
                .when(approveCourseCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseApprovedByAdminEvent(event))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void handleCourseApprovedByAdminEvent_businessException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(approveCourseCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseApprovedByAdminEvent(event))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void handleCourseApprovedByAdminEvent_pessimisticLockingException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doThrow(new PessimisticLockingFailureException("pessimistic lock"))
                .when(approveCourseCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseApprovedByAdminEvent(event))
                .isInstanceOf(PessimisticLockingFailureException.class);
    }

    @Test
    void handleCourseApprovedByAdminEvent_successfulHandling_doesNotPersistFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);

        // when
        sut.handleCourseApprovedByAdminEvent(event);

        // then
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void recover_persistsFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("DB connection lost");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getEventClassName()).isEqualTo(CourseApprovedByAdminIntegrationEvent.class.getName());
        assertThat(failedEvent.getEventPayload()).isEqualTo(event.toString());
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("DB connection lost");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void recover_withNullExceptionMessage_usesExceptionClassName() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException(null);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getExceptionMessage()).isEqualTo(OptimisticLockingFailureException.class.getName());
    }

}
