package com.educational.platform.course.enrollments.course.create;

import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;

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
public class CreateEnrollmentCourseCommandHandlerTest {

    @Mock
    private EnrollCourseRepository courseRepository;

    private CreateEnrollmentCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new CreateEnrollmentCourseCommandHandler(courseRepository);
    }

    @Test
    void handle_validCommand_courseSaved() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseCommand command = new CreateCourseCommand(courseUuid);

        // when
        sut.handle(command);

        // then
        ArgumentCaptor<EnrollCourse> captor = ArgumentCaptor.forClass(EnrollCourse.class);
        verify(courseRepository).save(captor.capture());
        final EnrollCourse savedCourse = captor.getValue();
        assertThat(savedCourse.toReference()).isEqualTo(courseUuid);
    }

    @Test
    void handle_nullUuidInCommand_courseSavedWithNullReference() {
        // given
        final CreateCourseCommand command = new CreateCourseCommand(null);

        // when
        sut.handle(command);

        // then
        ArgumentCaptor<EnrollCourse> captor = ArgumentCaptor.forClass(EnrollCourse.class);
        verify(courseRepository).save(captor.capture());
        final EnrollCourse savedCourse = captor.getValue();
        assertThat(savedCourse.toReference()).isNull();
    }

    @Test
    void handle_differentUuids_savedWithCorrectReference() {
        // given
        final UUID courseUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        final CreateCourseCommand command = new CreateCourseCommand(courseUuid);

        // when
        sut.handle(command);

        // then
        ArgumentCaptor<EnrollCourse> captor = ArgumentCaptor.forClass(EnrollCourse.class);
        verify(courseRepository).save(captor.capture());
        final EnrollCourse savedCourse = captor.getValue();
        assertThat(savedCourse.toReference()).isEqualTo(courseUuid);
    }
}
