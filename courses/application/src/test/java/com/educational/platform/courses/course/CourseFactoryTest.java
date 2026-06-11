package com.educational.platform.courses.course;


import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseFactoryTest {

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    private CourseFactory sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void createFrom_validCourse_courseCreated() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "name")
                .hasFieldOrPropertyWithValue("description", "description");
    }


    @Test
    void createFrom_nameIsNull_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name(null)
                .description("description")
                .build();

        // when
        final Executable createAction = () -> sut.createFrom(command);

        // then
        assertThrows(ConstraintViolationException.class, createAction);
    }


    @Test
    void createFrom_nameIsBlank_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("")
                .description("description")
                .build();

        // when
        final Executable createAction = () -> sut.createFrom(command);

        // then
        assertThrows(ConstraintViolationException.class, createAction);
    }


    @Test
    void createFrom_descriptionIsNull_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description(null)
                .build();

        // when
        final Executable createAction = () -> sut.createFrom(command);

        // then
        assertThrows(ConstraintViolationException.class, createAction);
    }


    @Test
    void createFrom_descriptionIsBlank_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("")
                .build();

        // when
        final Executable createAction = () -> sut.createFrom(command);

        // then
        assertThrows(ConstraintViolationException.class, createAction);
    }

    @Test
    void createFrom_bothFieldsNull_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name(null)
                .description(null)
                .build();

        // when
        final Executable createAction = () -> sut.createFrom(command);

        // then
        assertThrows(ConstraintViolationException.class, createAction);
    }

    @Test
    void createFrom_validCourse_courseHasTeacherId() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(42);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("teacher", 42);
    }

    @Test
    void createFrom_validCourse_courseHasInitialState() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0))
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0));
    }

    @Test
    void createFrom_validCourse_courseHasUuid() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = sut.createFrom(command);

        // then
        assertThat(course.toIdentity()).isNotNull();
    }

    @Test
    void createFrom_validCourseWithCurriculumItems_curriculumItemsPopulated() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Intro")
                .description("Intro lecture")
                .serialNumber(1)
                .text("Content")
                .build();
        final CreateQuestionCommand question = new CreateQuestionCommand("Q1");
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Quiz desc")
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
        final Course course = sut.createFrom(command);

        // then
        assertThat(course)
                .extracting("curriculumItems")
                .asList()
                .hasSize(2);
    }

    @Test
    void createFrom_twoCalls_differentUuids() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course1 = sut.createFrom(command);
        final Course course2 = sut.createFrom(command);

        // then
        assertThat(course1.toIdentity()).isNotEqualTo(course2.toIdentity());
    }

    @Test
    void createFrom_whitespaceOnlyName_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("   ")
                .description("description")
                .build();

        // when
        final Executable createAction = () -> sut.createFrom(command);

        // then
        assertThrows(ConstraintViolationException.class, createAction);
    }

}
