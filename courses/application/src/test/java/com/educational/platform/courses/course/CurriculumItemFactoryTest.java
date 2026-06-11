package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import com.educational.platform.courses.teacher.Teacher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurriculumItemFactoryTest {

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    private CourseFactory courseFactory;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        courseFactory = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void createFrom_lectureCommand_lectureCreated() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateLectureCommand lectureCommand = CreateLectureCommand.builder()
                .title("Lecture Title")
                .description("Lecture Description")
                .serialNumber(1)
                .text("Lecture Content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", "Lecture Title")
                .hasFieldOrPropertyWithValue("description", "Lecture Description")
                .hasFieldOrPropertyWithValue("serialNumber", 1);
    }

    @Test
    void createFrom_quizCommand_quizCreated() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz Title")
                .description("Quiz Description")
                .serialNumber(2)
                .text("Quiz Content")
                .questions(List.of(new CreateQuestionCommand("Question 1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", "Quiz Title")
                .hasFieldOrPropertyWithValue("description", "Quiz Description")
                .hasFieldOrPropertyWithValue("serialNumber", 2);
    }

    @Test
    void createFrom_lectureCommand_contentFieldPopulated() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateLectureCommand lectureCommand = CreateLectureCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(1)
                .text("Lecture body content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result)
                .isInstanceOf(Lecture.class)
                .hasFieldOrPropertyWithValue("content", "Lecture body content");
    }

    @Test
    void createFrom_quizCommand_multipleQuestions_allQuestionsMapped() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz Title")
                .description("Quiz Desc")
                .serialNumber(1)
                .text("Quiz Content")
                .questions(List.of(
                        new CreateQuestionCommand("Q1"),
                        new CreateQuestionCommand("Q2"),
                        new CreateQuestionCommand("Q3")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(3)
                .extracting("content")
                .containsExactly("Q1", "Q2", "Q3");
    }

    @Test
    void createFrom_lectureCommand_uuidGenerated() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateLectureCommand lectureCommand = CreateLectureCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(1)
                .text("Content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).hasFieldOrProperty("uuid");
        assertThat(result).extracting("uuid").isNotNull();
    }

    @Test
    void createFrom_lectureCommand_courseAssociationSet() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateLectureCommand lectureCommand = CreateLectureCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(1)
                .text("Content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).extracting("course").isSameAs(course);
    }

    @Test
    void createFrom_quizCommand_emptyQuestionsList_quizCreatedWithNoQuestions() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Empty Quiz")
                .description("Quiz with no questions")
                .serialNumber(1)
                .text("Quiz Content")
                .questions(Collections.emptyList())
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .isEmpty();
    }

    @Test
    void createFrom_quizCommand_uuidGeneratedAndCourseAssociationSet() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz Title")
                .description("Quiz Desc")
                .serialNumber(1)
                .text("Quiz Content")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).extracting("uuid").isNotNull();
        assertThat(result).extracting("course").isSameAs(course);
    }

    @Test
    void createFrom_nullCommand_nullReturned() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        // when — anonymous subclass that is neither Lecture nor Quiz
        final CurriculumItem result = CurriculumItemFactory.createFrom(
                new com.educational.platform.courses.course.create.CreateCurriculumItemCommand("title", "desc", 3) {},
                course);

        // then
        assertThat(result).isNull();
    }

    @Test
    void createFrom_nullCommandArgument_returnsNull() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(null, course);

        // then
        assertThat(result).isNull();
    }

    @Test
    void createFrom_quizCommand_nullQuestionsList_throwsNullPointerException() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz Title")
                .description("Quiz Desc")
                .serialNumber(1)
                .text("Quiz Content")
                .questions(null)
                .build();

        // when / then
        assertThatThrownBy(() -> CurriculumItemFactory.createFrom(quizCommand, course))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void createFrom_twoLecturesFromSameCourse_eachHasDistinctUuid() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateLectureCommand lectureCommand1 = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("Desc 1")
                .serialNumber(1)
                .text("Content 1")
                .build();
        final CreateLectureCommand lectureCommand2 = CreateLectureCommand.builder()
                .title("Lecture 2")
                .description("Desc 2")
                .serialNumber(2)
                .text("Content 2")
                .build();

        // when
        final CurriculumItem result1 = CurriculumItemFactory.createFrom(lectureCommand1, course);
        final CurriculumItem result2 = CurriculumItemFactory.createFrom(lectureCommand2, course);

        // then
        assertThat(List.of(result1, result2))
                .extracting("uuid")
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }

    @Test
    void createFrom_quizCommand_singleQuestion_questionHasQuizBackReference() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz Title")
                .description("Quiz Desc")
                .serialNumber(1)
                .text("Quiz Content")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(1)
                .first()
                .extracting("quiz")
                .isSameAs(result);
    }

}
