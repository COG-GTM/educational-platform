package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CourseApprovedByAdminIntegrationEventHandlerUnitTest {

    @Mock
    private ApproveCourseCommandHandler approveCourseCommandHandler;

    private CourseApprovedByAdminIntegrationEventHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CourseApprovedByAdminIntegrationEventHandler(approveCourseCommandHandler);
    }

    @Test
    void handleCourseApprovedByAdminEvent_delegatesToCommandHandler() {
        // given
        final UUID courseId = UUID.randomUUID();
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(courseId);

        // when
        sut.handleCourseApprovedByAdminEvent(event);

        // then
        final ArgumentCaptor<ApproveCourseCommand> captor = ArgumentCaptor.forClass(ApproveCourseCommand.class);
        verify(approveCourseCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(courseId);
    }
}
