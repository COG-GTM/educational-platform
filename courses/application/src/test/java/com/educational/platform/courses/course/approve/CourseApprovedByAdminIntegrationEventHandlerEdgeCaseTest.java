package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.common.exception.ResourceNotFoundException;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseApprovedByAdminIntegrationEventHandlerEdgeCaseTest {

    @Mock
    private ApproveCourseCommandHandler approveCourseCommandHandler;

    @InjectMocks
    private CourseApprovedByAdminIntegrationEventHandler sut;

    @Test
    void handleCourseApprovedByAdminEvent_handlerThrowsResourceNotFound_exceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doThrow(new ResourceNotFoundException("Course with uuid: " + uuid + " not found"))
                .when(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handleCourseApprovedByAdminEvent(event);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }

    @Test
    void handleCourseApprovedByAdminEvent_validEvent_commandContainsCorrectUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);

        // when
        sut.handleCourseApprovedByAdminEvent(event);

        // then
        final ArgumentCaptor<ApproveCourseCommand> argument = ArgumentCaptor.forClass(ApproveCourseCommand.class);
        verify(approveCourseCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().uuid()).isEqualTo(uuid);
    }
}
