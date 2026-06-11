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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
    }

    private void stubTeacher() {
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
    }

    @Test
    void handle_validCourse_saveExecuted() {
        // given
        stubTeacher();
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
    void handle_validCourse_returnsNonNullUuid() {
        // given
        stubTeacher();
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
    void handle_invalidCommand_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name(null)
                .description(null)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(command));
    }

    @Test
    void handle_validCourseWithCurriculumItems_saveExecuted() {
        // given
        stubTeacher();
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Intro")
                .description("Introduction lecture")
                .serialNumber(1)
                .text("Content text")
                .build();
        final CreateQuestionCommand question = new CreateQuestionCommand("Q1");
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("First quiz")
                .serialNumber(2)
                .text("Quiz text")
                .questions(List.of(question))
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(List.of(lecture, quiz))
                .build();

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        ArgumentCaptor<Course> argument = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(argument.capture());
        final Course course = argument.getValue();
        assertThat(course)
                .extracting("curriculumItems")
                .asList()
                .hasSize(2);
    }

    @Test
    void handle_validCourse_saveCalledExactlyOnce() {
        // given
        stubTeacher();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        sut.handle(command);

        // then
        verify(repository, times(1)).save(org.mockito.ArgumentMatchers.any(Course.class));
    }

    @Test
    void handle_twoSequentialCalls_returnDifferentUuids() {
        // given
        stubTeacher();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final UUID first = sut.handle(command);
        final UUID second = sut.handle(command);

        // then
        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void handle_invalidCommand_saveNotCalled() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name(null)
                .description(null)
                .build();

        // when / then
        try {
            sut.handle(command);
        } catch (ConstraintViolationException ignored) {
        }

        // then
        verify(repository, times(0)).save(org.mockito.ArgumentMatchers.any(Course.class));
    }
}
