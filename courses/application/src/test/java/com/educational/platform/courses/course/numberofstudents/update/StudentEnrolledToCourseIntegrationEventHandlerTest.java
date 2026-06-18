package com.educational.platform.courses.course.numberofstudents.update;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;

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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StudentEnrolledToCourseIntegrationEventHandlerTest {

    @Mock
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @InjectMocks
    private StudentEnrolledToCourseIntegrationEventHandler sut;


    @Test
    void handleStudentEnrolledToCourseEvent_updateNumberOfStudentsCommandExecuted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> argument = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(argument.capture());
        final IncreaseNumberOfStudentsCommand updateNumberOfStudentsCommand = argument.getValue();
        assertThat(updateNumberOfStudentsCommand)
                .hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_usernameIgnored_courseIdStillMapped() {
        // given - the handler only enrolls against the course, the event username is not used
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, null);

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> argument = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_commandHandlerThrows_exceptionPropagated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new RuntimeException("number of students could not be increased"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any(IncreaseNumberOfStudentsCommand.class));

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("number of students could not be increased");
    }

}
