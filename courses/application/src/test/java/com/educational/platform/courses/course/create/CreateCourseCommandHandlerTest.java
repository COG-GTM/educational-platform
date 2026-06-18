package com.educational.platform.courses.course.create;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseFactory;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.CurrentUserAsTeacher;
import com.educational.platform.courses.teacher.Teacher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateCourseCommandHandlerTest {

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

        var teacher = mock(Teacher.class);
        lenient().when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        lenient().when(teacher.getId()).thenReturn(15);
    }

    @Test
    void handle_validCourse_saveExecuted() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        sut.handle(command);

        // then
        ArgumentCaptor<Course> argument = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(argument.capture());
        final Course course = argument.getValue();
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "name")
                .hasFieldOrPropertyWithValue("description", "description");
    }

    @Test
    void handle_validCommand_savedCourseIdentityReturned() {
        // given - the create flow returns the natural key of the freshly persisted course (the uuid other
        // modules use to look it up); the existing happy-path test only asserts the course is saved
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final UUID result = sut.handle(command);

        // then
        final ArgumentCaptor<Course> argument = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(argument.capture());
        assertThat(result).isEqualTo(argument.getValue().toIdentity());
    }

    @Test
    void handle_blankName_constraintViolationExceptionAndCourseNotSaved() {
        // given - validation is delegated to the factory; a blank name violates the command's @NotBlank
        // contract, so the handler must propagate the failure and persist nothing
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("")
                .description("description")
                .build();

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(ConstraintViolationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void handle_repositoryThrows_exceptionPropagated() {
        // given - a persistence failure must propagate rather than be swallowed, consistent with the
        // save-failure contract covered for the other create command handlers
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        doThrow(new RuntimeException("course could not be saved"))
                .when(repository).save(any(Course.class));

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("course could not be saved");
    }
}
