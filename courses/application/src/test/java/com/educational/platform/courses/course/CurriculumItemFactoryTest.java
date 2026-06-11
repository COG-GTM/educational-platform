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

    @Test
    void createFrom_lectureCommand_nullCourse_lectureCreatedWithNullCourse() {
        // given
        final CreateLectureCommand lectureCommand = CreateLectureCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(1)
                .text("Content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, null);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result).extracting("course").isNull();
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", "Title")
                .hasFieldOrPropertyWithValue("serialNumber", 1);
    }

    @Test
    void createFrom_quizCommand_nullCourse_quizCreatedWithNullCourse() {
        // given
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("Content")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, null);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("course").isNull();
    }

    @Test
    void createFrom_lectureCommand_nullFields_fieldsAreNull() {
        // given
        final CreateLectureCommand lectureCommand = CreateLectureCommand.builder()
                .title(null)
                .description(null)
                .serialNumber(null)
                .text(null)
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, null);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", null)
                .hasFieldOrPropertyWithValue("description", null)
                .hasFieldOrPropertyWithValue("serialNumber", null)
                .hasFieldOrPropertyWithValue("content", null);
    }

    @Test
    void createFrom_lectureCommand_serialNumberZero_accepted() {
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
                .serialNumber(0)
                .text("Content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result).hasFieldOrPropertyWithValue("serialNumber", 0);
    }

    @Test
    void createFrom_quizCommand_nullFields_fieldsAreNull() {
        // given
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title(null)
                .description(null)
                .serialNumber(null)
                .text(null)
                .questions(List.of())
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, null);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", null)
                .hasFieldOrPropertyWithValue("description", null)
                .hasFieldOrPropertyWithValue("serialNumber", null);
    }

    @Test
    void createFrom_twoQuizzesFromSameCourse_eachHasDistinctUuid() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateQuizCommand quizCommand1 = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("Desc 1")
                .serialNumber(1)
                .text("Content 1")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();
        final CreateQuizCommand quizCommand2 = CreateQuizCommand.builder()
                .title("Quiz 2")
                .description("Desc 2")
                .serialNumber(2)
                .text("Content 2")
                .questions(List.of(new CreateQuestionCommand("Q2")))
                .build();

        // when
        final CurriculumItem result1 = CurriculumItemFactory.createFrom(quizCommand1, course);
        final CurriculumItem result2 = CurriculumItemFactory.createFrom(quizCommand2, course);

        // then
        assertThat(List.of(result1, result2))
                .extracting("uuid")
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }

    @Test
    void createFrom_lectureCommand_negativeSerialNumber_accepted() {
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
                .serialNumber(-1)
                .text("Content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result).hasFieldOrPropertyWithValue("serialNumber", -1);
    }

    @Test
    void createFrom_lectureAndQuizFromSameCourse_bothHaveDistinctUuids() {
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
                .title("Lecture")
                .description("Desc")
                .serialNumber(1)
                .text("Content")
                .build();
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(2)
                .text("Content")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem lecture = CurriculumItemFactory.createFrom(lectureCommand, course);
        final CurriculumItem quiz = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(List.of(lecture, quiz))
                .extracting("uuid")
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }

    @Test
    void createFrom_quizCommand_serialNumberZero_accepted() {
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
                .title("Quiz")
                .description("Desc")
                .serialNumber(0)
                .text("Content")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).hasFieldOrPropertyWithValue("serialNumber", 0);
    }

    @Test
    void createFrom_quizCommand_questionsOrderPreserved() {
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
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("Content")
                .questions(List.of(
                        new CreateQuestionCommand("Zebra"),
                        new CreateQuestionCommand("Apple"),
                        new CreateQuestionCommand("Mango")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .extracting("content")
                .containsExactly("Zebra", "Apple", "Mango");
    }

    @Test
    void createFrom_quizCommand_textFieldNotStoredOnQuiz() {
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
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("This text should not appear on quiz")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).doesNotHaveToString("This text should not appear on quiz");
        assertThat(result).extracting("title").isEqualTo("Quiz");
    }

    @Test
    void createFrom_quizCommand_nullQuestionContent_questionCreatedWithNullContent() {
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
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("Content")
                .questions(List.of(new CreateQuestionCommand(null)))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("content", null);
    }

    @Test
    void createFrom_lectureCommand_maxIntSerialNumber_accepted() {
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
                .serialNumber(Integer.MAX_VALUE)
                .text("Content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result).hasFieldOrPropertyWithValue("serialNumber", Integer.MAX_VALUE);
    }

    @Test
    void createFrom_quizCommand_duplicateQuestionContent_allDuplicatesMapped() {
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
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("Content")
                .questions(List.of(
                        new CreateQuestionCommand("Same"),
                        new CreateQuestionCommand("Same"),
                        new CreateQuestionCommand("Same")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(3)
                .extracting("content")
                .containsExactly("Same", "Same", "Same");
    }

    @Test
    void createFrom_quizCommand_negativeSerialNumber_accepted() {
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
                .title("Quiz")
                .description("Desc")
                .serialNumber(-1)
                .text("Content")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).hasFieldOrPropertyWithValue("serialNumber", -1);
    }

    @Test
    void createFrom_quizCommand_maxIntSerialNumber_accepted() {
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
                .title("Quiz")
                .description("Desc")
                .serialNumber(Integer.MAX_VALUE)
                .text("Content")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).hasFieldOrPropertyWithValue("serialNumber", Integer.MAX_VALUE);
    }

    @Test
    void createFrom_lectureCommand_emptyStringContent_storedAsEmptyString() {
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
                .text("")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result).hasFieldOrPropertyWithValue("content", "");
    }

    @Test
    void createFrom_quizCommand_largeQuestionsList_allMapped() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final List<CreateQuestionCommand> questions = java.util.stream.IntStream.rangeClosed(1, 50)
                .mapToObj(i -> new CreateQuestionCommand("Q" + i))
                .toList();
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("Content")
                .questions(questions)
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(50);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .first()
                .hasFieldOrPropertyWithValue("content", "Q1");
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .last()
                .hasFieldOrPropertyWithValue("content", "Q50");
    }

    @Test
    void createFrom_quizCommand_descriptionFieldStored() {
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
                .description("Detailed quiz description")
                .serialNumber(1)
                .text("Content")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).hasFieldOrPropertyWithValue("description", "Detailed quiz description");
    }

    @Test
    void createFrom_quizCommand_multipleQuestions_allHaveQuizBackReference() {
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
                .text("Content")
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
                .allSatisfy(question ->
                        assertThat(question).extracting("quiz").isSameAs(result));
    }

    @Test
    void createFrom_lectureCommand_descriptionFieldStored() {
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
                .description("Detailed lecture description")
                .serialNumber(1)
                .text("Content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result).hasFieldOrPropertyWithValue("description", "Detailed lecture description");
    }

    @Test
    void createFrom_lectureCommand_minIntSerialNumber_accepted() {
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
                .serialNumber(Integer.MIN_VALUE)
                .text("Content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result).hasFieldOrPropertyWithValue("serialNumber", Integer.MIN_VALUE);
    }

    @Test
    void createFrom_quizCommand_minIntSerialNumber_accepted() {
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
                .title("Quiz")
                .description("Desc")
                .serialNumber(Integer.MIN_VALUE)
                .text("Content")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).hasFieldOrPropertyWithValue("serialNumber", Integer.MIN_VALUE);
    }

    @Test
    void createFrom_nullCommandAndNullCourse_returnsNull() {
        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(null, null);

        // then
        assertThat(result).isNull();
    }

    @Test
    void createFrom_lectureCommand_allFieldsDistinct_correctlyMapped() {
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
                .title("TITLE_VALUE")
                .description("DESCRIPTION_VALUE")
                .serialNumber(99)
                .text("CONTENT_VALUE")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", "TITLE_VALUE")
                .hasFieldOrPropertyWithValue("description", "DESCRIPTION_VALUE")
                .hasFieldOrPropertyWithValue("serialNumber", 99)
                .hasFieldOrPropertyWithValue("content", "CONTENT_VALUE");
    }

    @Test
    void createFrom_quizCommand_allFieldsDistinct_correctlyMapped() {
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
                .title("QUIZ_TITLE")
                .description("QUIZ_DESCRIPTION")
                .serialNumber(77)
                .text("QUIZ_TEXT")
                .questions(List.of(new CreateQuestionCommand("QUESTION_CONTENT")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", "QUIZ_TITLE")
                .hasFieldOrPropertyWithValue("description", "QUIZ_DESCRIPTION")
                .hasFieldOrPropertyWithValue("serialNumber", 77);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("content", "QUESTION_CONTENT");
    }

    @Test
    void createFrom_lectureCommand_whitespaceOnlyFields_preserved() {
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
                .title("   ")
                .description("\t\n")
                .serialNumber(1)
                .text("  \n  ")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", "   ")
                .hasFieldOrPropertyWithValue("description", "\t\n")
                .hasFieldOrPropertyWithValue("content", "  \n  ");
    }

    @Test
    void createFrom_lecturesFromDifferentCourses_eachHasDistinctUuids() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);

        final Course course1 = courseFactory.createFrom(CreateCourseCommand.builder()
                .name("course1").description("desc1").build());
        final Course course2 = courseFactory.createFrom(CreateCourseCommand.builder()
                .name("course2").description("desc2").build());

        final CreateLectureCommand lectureCmd = CreateLectureCommand.builder()
                .title("Title").description("Desc").serialNumber(1).text("Content").build();

        // when
        final CurriculumItem result1 = CurriculumItemFactory.createFrom(lectureCmd, course1);
        final CurriculumItem result2 = CurriculumItemFactory.createFrom(lectureCmd, course2);

        // then
        assertThat(List.of(result1, result2))
                .extracting("uuid")
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }

    @Test
    void createFrom_twoLecturesWithSameSerialNumber_bothCreatedIndependently() {
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
                .title("Lecture A")
                .description("Desc A")
                .serialNumber(1)
                .text("Content A")
                .build();
        final CreateLectureCommand lectureCommand2 = CreateLectureCommand.builder()
                .title("Lecture B")
                .description("Desc B")
                .serialNumber(1)
                .text("Content B")
                .build();

        // when
        final CurriculumItem result1 = CurriculumItemFactory.createFrom(lectureCommand1, course);
        final CurriculumItem result2 = CurriculumItemFactory.createFrom(lectureCommand2, course);

        // then
        assertThat(result1).isInstanceOf(Lecture.class);
        assertThat(result2).isInstanceOf(Lecture.class);
        assertThat(result1).hasFieldOrPropertyWithValue("serialNumber", 1);
        assertThat(result2).hasFieldOrPropertyWithValue("serialNumber", 1);
        assertThat(result1).hasFieldOrPropertyWithValue("title", "Lecture A");
        assertThat(result2).hasFieldOrPropertyWithValue("title", "Lecture B");
        assertThat(result1).extracting("uuid").isNotEqualTo(result2.toString());
    }

    @Test
    void createFrom_quizCommand_singleQuestion_contentPreserved() {
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
                .questions(List.of(new CreateQuestionCommand("The Only Question")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("content", "The Only Question");
    }

}
