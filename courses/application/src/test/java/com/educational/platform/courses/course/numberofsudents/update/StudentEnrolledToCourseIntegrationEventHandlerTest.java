package com.educational.platform.courses.course.numberofsudents.update;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
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
public class StudentEnrolledToCourseIntegrationEventHandlerTest {

    @Mock
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    private StudentEnrolledToCourseIntegrationEventHandler sut;

    @BeforeEach
    void setUp() {
        sut = new StudentEnrolledToCourseIntegrationEventHandler(increaseNumberOfStudentsCommandHandler);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_delegatesToIncreaseHandlerWithCourseId() {
        // given
        final UUID courseId = UUID.randomUUID();
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "username");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> captor = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(courseId);
    }
}
