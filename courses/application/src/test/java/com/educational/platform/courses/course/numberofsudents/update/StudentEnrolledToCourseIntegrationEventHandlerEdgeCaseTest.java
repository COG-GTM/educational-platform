package com.educational.platform.courses.course.numberofsudents.update;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentEnrolledToCourseIntegrationEventHandlerEdgeCaseTest {

    @Mock
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @InjectMocks
    private StudentEnrolledToCourseIntegrationEventHandler sut;

    @Test
    void handleStudentEnrolledToCourseEvent_validEvent_commandHasCorrectCourseId() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "student-username");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> argument = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("uuid", courseId);
    }
}
