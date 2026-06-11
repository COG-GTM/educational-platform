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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CourseApprovedByAdminIntegrationEventHandlerTest {

    @Mock
    private ApproveCourseCommandHandler approveCourseCommandHandler;

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
    void handleCourseApprovedByAdminEvent_differentUuid_correctUuidPassed() {
        // given
        final UUID uuid = UUID.fromString("abcdef01-2345-6789-abcd-ef0123456789");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);

        // when
        sut.handleCourseApprovedByAdminEvent(event);

        // then
        final ArgumentCaptor<ApproveCourseCommand> argument = ArgumentCaptor.forClass(ApproveCourseCommand.class);
        verify(approveCourseCommandHandler).handle(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void handleCourseApprovedByAdminEvent_handlerThrowsException_exceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);
        doThrow(new RuntimeException("handler error")).when(approveCourseCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseApprovedByAdminEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("handler error");
    }

    @Test
    void handleCourseApprovedByAdminEvent_handlerCalledExactlyOnce() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);

        // when
        sut.handleCourseApprovedByAdminEvent(event);

        // then
        verify(approveCourseCommandHandler, times(1)).handle(any(ApproveCourseCommand.class));
    }

}
