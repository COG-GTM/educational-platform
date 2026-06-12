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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateEnrollmentCourseCommandHandlerEdgeCaseTest {

    @Mock
    private EnrollCourseRepository courseRepository;

    @InjectMocks
    private CreateEnrollmentCourseCommandHandler sut;

    @Test
    void handle_twoDifferentCourses_bothSaved() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CreateCourseCommand command1 = new CreateCourseCommand(uuid1);
        final CreateCourseCommand command2 = new CreateCourseCommand(uuid2);

        // when
        sut.handle(command1);
        sut.handle(command2);

        // then
        final ArgumentCaptor<EnrollCourse> argument = ArgumentCaptor.forClass(EnrollCourse.class);
        verify(courseRepository, times(2)).save(argument.capture());
        assertThat(argument.getAllValues()).hasSize(2);
        assertThat(argument.getAllValues().get(0)).hasFieldOrPropertyWithValue("uuid", uuid1);
        assertThat(argument.getAllValues().get(1)).hasFieldOrPropertyWithValue("uuid", uuid2);
    }
}
