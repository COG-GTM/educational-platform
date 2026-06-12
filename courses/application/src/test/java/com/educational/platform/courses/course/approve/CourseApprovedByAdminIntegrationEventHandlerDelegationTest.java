package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CourseApprovedByAdminIntegrationEventHandlerDelegationTest {

    @Mock
    private ApproveCourseCommandHandler approveCourseCommandHandler;

    @InjectMocks
    private CourseApprovedByAdminIntegrationEventHandler sut;

    @Test
    void handleCourseApprovedByAdminEvent_validEvent_delegatesToHandlerWithCorrectUuid() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(courseId);

        // when
        sut.handleCourseApprovedByAdminEvent(event);

        // then
        final ArgumentCaptor<ApproveCourseCommand> argument = ArgumentCaptor.forClass(ApproveCourseCommand.class);
        verify(approveCourseCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().uuid()).isEqualTo(courseId);
    }

    @Test
    void handleCourseApprovedByAdminEvent_validEvent_noAdditionalInteractions() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(courseId);

        // when
        sut.handleCourseApprovedByAdminEvent(event);

        // then
        verify(approveCourseCommandHandler).handle(org.mockito.ArgumentMatchers.any(ApproveCourseCommand.class));
        verifyNoMoreInteractions(approveCourseCommandHandler);
    }
}
