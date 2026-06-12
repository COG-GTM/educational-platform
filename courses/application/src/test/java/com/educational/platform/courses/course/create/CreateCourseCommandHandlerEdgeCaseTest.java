package com.educational.platform.courses.course.create;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseFactory;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.CurrentUserAsTeacher;
import com.educational.platform.courses.teacher.Teacher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCourseCommandHandlerEdgeCaseTest {

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    @Mock
    private CourseRepository repository;

    private CreateCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final CourseFactory courseFactory = new CourseFactory(validator, currentUserAsTeacher);
        sut = new CreateCourseCommandHandler(repository, courseFactory);
    }

    @Test
    void handle_validCommand_returnsNonNullUuid() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    void handle_twoCalls_differentUuids() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand command1 = CreateCourseCommand.builder()
                .name("name1")
                .description("description1")
                .build();
        final CreateCourseCommand command2 = CreateCourseCommand.builder()
                .name("name2")
                .description("description2")
                .build();

        // when
        final UUID uuid1 = sut.handle(command1);
        final UUID uuid2 = sut.handle(command2);

        // then
        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    @Test
    void handle_blankName_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("")
                .description("description")
                .build();

        // when
        final Executable handle = () -> sut.handle(command);

        // then
        assertThrows(ConstraintViolationException.class, handle);
    }

    @Test
    void handle_nullDescription_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description(null)
                .build();

        // when
        final Executable handle = () -> sut.handle(command);

        // then
        assertThrows(ConstraintViolationException.class, handle);
    }
}
