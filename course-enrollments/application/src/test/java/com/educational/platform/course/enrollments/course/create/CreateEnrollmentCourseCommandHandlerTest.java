package com.educational.platform.course.enrollments.course.create;

import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;

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
public class CreateEnrollmentCourseCommandHandlerTest {

    @Mock
    private EnrollCourseRepository courseRepository;

    @InjectMocks
    private CreateEnrollmentCourseCommandHandler sut;

    @Test
    void handle_validCommand_courseSaved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseCommand command = new CreateCourseCommand(uuid);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<EnrollCourse> argument = ArgumentCaptor.forClass(EnrollCourse.class);
        verify(courseRepository).save(argument.capture());
        final EnrollCourse course = argument.getValue();
        assertThat(course)
                .hasFieldOrPropertyWithValue("uuid", uuid);
        assertThat(course.toReference()).isEqualTo(uuid);
    }
}
