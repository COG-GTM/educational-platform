package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.teacher.Teacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseFactoryTest {

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    @Mock
    private Teacher teacher;

    private CourseFactory sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void createFrom_validCommand_returnsCourseWithCorrectFields() {
        // given
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(42);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Mathematics")
                .description("Advanced math course")
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course).isNotNull();
        assertThat(course.toIdentity()).isNotNull();
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "Mathematics")
                .hasFieldOrPropertyWithValue("description", "Advanced math course")
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0))
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0))
                .hasFieldOrPropertyWithValue("teacher", 42);
    }

    @Test
    void createFrom_commandWithCurriculumItems_courseContainsItems() {
        // given
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(1);
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("First lecture")
                .serialNumber(1)
                .text("Content")
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Physics")
                .description("Intro to physics")
                .curriculumItems(List.of(lecture))
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course).isNotNull();
        assertThat(course).hasFieldOrPropertyWithValue("name", "Physics");
    }

    @Test
    void createFrom_blankName_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("")
                .description("description")
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_nullName_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name(null)
                .description("description")
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_nullDescription_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description(null)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_bothFieldsBlank_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("")
                .description("")
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }
}
