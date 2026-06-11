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

    @Test
    void createFrom_sameCommandReusedForTwoLectures_independentItemsWithDistinctUuids() {
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
                .title("Reused Title")
                .description("Reused Desc")
                .serialNumber(1)
                .text("Reused Content")
                .build();

        // when
        final CurriculumItem result1 = CurriculumItemFactory.createFrom(lectureCommand, course);
        final CurriculumItem result2 = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result1).isInstanceOf(Lecture.class);
        assertThat(result2).isInstanceOf(Lecture.class);
        assertThat(result1).isNotSameAs(result2);
        assertThat(List.of(result1, result2))
                .extracting("uuid")
                .doesNotContainNull()
                .doesNotHaveDuplicates();
        assertThat(result1).hasFieldOrPropertyWithValue("title", "Reused Title");
        assertThat(result2).hasFieldOrPropertyWithValue("title", "Reused Title");
    }

    @Test
    void createFrom_sameQuizCommandReusedTwice_independentItemsWithDistinctUuids() {
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
                .title("Reused Quiz")
                .description("Reused Desc")
                .serialNumber(1)
                .text("Content")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result1 = CurriculumItemFactory.createFrom(quizCommand, course);
        final CurriculumItem result2 = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result1).isInstanceOf(Quiz.class);
        assertThat(result2).isInstanceOf(Quiz.class);
        assertThat(result1).isNotSameAs(result2);
        assertThat(List.of(result1, result2))
                .extracting("uuid")
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }

    @Test
    void createFrom_quizCommand_emptyStringQuestionContent_storedAsEmptyString() {
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
                .questions(List.of(new CreateQuestionCommand("")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("content", "");
    }

    @Test
    void createFrom_lectureCommand_veryLongStrings_allPreserved() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final String longTitle = "T".repeat(1000);
        final String longDesc = "D".repeat(5000);
        final String longContent = "C".repeat(10000);
        final CreateLectureCommand lectureCommand = CreateLectureCommand.builder()
                .title(longTitle)
                .description(longDesc)
                .serialNumber(1)
                .text(longContent)
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", longTitle)
                .hasFieldOrPropertyWithValue("description", longDesc)
                .hasFieldOrPropertyWithValue("content", longContent);
    }

    @Test
    void createFrom_multipleItemsCreatedSequentially_allUuidsDistinct() {
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
        final java.util.List<CurriculumItem> items = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(i -> {
                    if (i % 2 == 0) {
                        return CurriculumItemFactory.createFrom(
                                CreateLectureCommand.builder()
                                        .title("Lecture " + i).description("Desc").serialNumber(i).text("Content").build(),
                                course);
                    } else {
                        return CurriculumItemFactory.createFrom(
                                CreateQuizCommand.builder()
                                        .title("Quiz " + i).description("Desc").serialNumber(i).text("Content")
                                        .questions(List.of(new CreateQuestionCommand("Q1"))).build(),
                                course);
                    }
                })
                .toList();

        // then
        assertThat(items).hasSize(10).doesNotContainNull();
        assertThat(items)
                .extracting("uuid")
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }

    @Test
    void createFrom_quizCommand_noContentFieldOnQuiz() {
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
                .text("Text that should not appear")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).hasFieldOrPropertyWithValue("title", "Quiz");
        assertThat(result).hasFieldOrPropertyWithValue("description", "Desc");
        assertThat(result).hasFieldOrPropertyWithValue("serialNumber", 1);
        // Quiz does not have a content field unlike Lecture
        assertThat(result.getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("content");
    }

    @Test
    void createFrom_lectureCommand_hasContentField() {
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
                .text("Actual Content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result).hasFieldOrPropertyWithValue("content", "Actual Content");
        assertThat(result.getClass().getDeclaredFields())
                .extracting("name")
                .contains("content");
    }

    @Test
    void createFrom_lectureCommand_emptyStringTitleAndDescription_preserved() {
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
                .title("")
                .description("")
                .serialNumber(1)
                .text("Content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", "")
                .hasFieldOrPropertyWithValue("description", "")
                .hasFieldOrPropertyWithValue("content", "Content");
    }

    @Test
    void createFrom_quizCommand_emptyStringQuestionContent_preserved() {
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
                .questions(List.of(new CreateQuestionCommand("")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("content", "");
    }

    @Test
    void createFrom_lectureCommand_unicodeContent_preservedInContentField() {
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
                .title("日本語タイトル")
                .description("Описание αβγ")
                .serialNumber(1)
                .text("内容 содержание \uD83D\uDCDA")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", "日本語タイトル")
                .hasFieldOrPropertyWithValue("description", "Описание αβγ")
                .hasFieldOrPropertyWithValue("content", "内容 содержание \uD83D\uDCDA");
    }

    @Test
    void createFrom_sameCommandInstanceReusedForDifferentCourses_independentItems() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);

        final Course course1 = courseFactory.createFrom(CreateCourseCommand.builder()
                .name("course1").description("desc1").build());
        final Course course2 = courseFactory.createFrom(CreateCourseCommand.builder()
                .name("course2").description("desc2").build());

        final CreateLectureCommand sharedCommand = CreateLectureCommand.builder()
                .title("Shared Title")
                .description("Shared Desc")
                .serialNumber(1)
                .text("Shared Content")
                .build();

        // when
        final CurriculumItem item1 = CurriculumItemFactory.createFrom(sharedCommand, course1);
        final CurriculumItem item2 = CurriculumItemFactory.createFrom(sharedCommand, course2);

        // then
        assertThat(item1).extracting("course").isSameAs(course1);
        assertThat(item2).extracting("course").isSameAs(course2);
        assertThat(item1).isNotSameAs(item2);
        assertThat(item1).extracting("uuid").isNotEqualTo(item2.getClass().getDeclaredFields());
        assertThat(List.of(item1, item2))
                .extracting("uuid")
                .doesNotHaveDuplicates();
    }

    @Test
    void createFrom_quizCommand_mixedNullAndNonNullQuestionContent_allPreserved() {
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
                        new CreateQuestionCommand("Valid"),
                        new CreateQuestionCommand(null),
                        new CreateQuestionCommand("")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(3)
                .extracting("content")
                .containsExactly("Valid", null, "");
    }

    @Test
    void createFrom_quizCommand_questionsFromSingletonList_singleQuestionCreated() {
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
                .questions(Collections.singletonList(new CreateQuestionCommand("Single")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("content", "Single");
    }

    @Test
    void createFrom_sameQuizCommandOnSameCourse_producesIndependentItems() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateQuizCommand sharedQuizCommand = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("Content")
                .questions(List.of(new CreateQuestionCommand("Q1"), new CreateQuestionCommand("Q2")))
                .build();

        // when
        final CurriculumItem item1 = CurriculumItemFactory.createFrom(sharedQuizCommand, course);
        final CurriculumItem item2 = CurriculumItemFactory.createFrom(sharedQuizCommand, course);

        // then
        assertThat(item1).isInstanceOf(Quiz.class);
        assertThat(item2).isInstanceOf(Quiz.class);
        assertThat(item1).isNotSameAs(item2);
        assertThat(List.of(item1, item2))
                .extracting("uuid")
                .doesNotHaveDuplicates();
        assertThat(item1).extracting("course").isSameAs(course);
        assertThat(item2).extracting("course").isSameAs(course);
    }

    @Test
    void createFrom_quizCommand_questionsListNotSharedBetweenItems() {
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
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem quiz1 = CurriculumItemFactory.createFrom(quizCommand, course);
        final CurriculumItem quiz2 = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(quiz1).extracting("questions").isNotSameAs(quiz2.getClass().cast(quiz2));
        assertThat(quiz1).extracting("questions").asInstanceOf(LIST).hasSize(1);
        assertThat(quiz2).extracting("questions").asInstanceOf(LIST).hasSize(1);
    }

    @Test
    void createFrom_lectureCommand_doesNotHaveQuestionsField() {
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
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result.getClass().getDeclaredFields())
                .extracting("name")
                .contains("content")
                .doesNotContain("questions");
    }

    @Test
    void createFrom_quizCommand_veryLongQuestionContent_preserved() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final String longQuestion = "Q".repeat(10000);
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("Content")
                .questions(List.of(new CreateQuestionCommand(longQuestion)))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("content", longQuestion);
    }

    @Test
    void createFrom_sameLectureCommandOnSameCourse_producesIndependentItems() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateLectureCommand sharedLectureCommand = CreateLectureCommand.builder()
                .title("Shared Lecture")
                .description("Shared Desc")
                .serialNumber(5)
                .text("Shared Content")
                .build();

        // when
        final CurriculumItem item1 = CurriculumItemFactory.createFrom(sharedLectureCommand, course);
        final CurriculumItem item2 = CurriculumItemFactory.createFrom(sharedLectureCommand, course);

        // then
        assertThat(item1).isInstanceOf(Lecture.class);
        assertThat(item2).isInstanceOf(Lecture.class);
        assertThat(item1).isNotSameAs(item2);
        assertThat(List.of(item1, item2))
                .extracting("uuid")
                .doesNotHaveDuplicates();
    }

    @Test
    void createFrom_quizCommand_nullCourse_questionsStillReferenceQuiz() {
        // given
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("Content")
                .questions(List.of(
                        new CreateQuestionCommand("Q1"),
                        new CreateQuestionCommand("Q2")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, null);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("course").isNull();
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(2)
                .allSatisfy(question ->
                        assertThat(question).extracting("quiz").isSameAs(result));
    }

    @Test
    void createFrom_lectureAndQuiz_sameTitleDescription_fieldsNotCrossContaminated() {
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
                .title("Shared Title")
                .description("Shared Desc")
                .serialNumber(1)
                .text("Lecture specific text")
                .build();
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Shared Title")
                .description("Shared Desc")
                .serialNumber(2)
                .text("Quiz specific text")
                .questions(List.of(new CreateQuestionCommand("Quiz Q")))
                .build();

        // when
        final CurriculumItem lecture = CurriculumItemFactory.createFrom(lectureCommand, course);
        final CurriculumItem quiz = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(lecture).isInstanceOf(Lecture.class);
        assertThat(lecture)
                .hasFieldOrPropertyWithValue("title", "Shared Title")
                .hasFieldOrPropertyWithValue("content", "Lecture specific text");
        assertThat(quiz).isInstanceOf(Quiz.class);
        assertThat(quiz).hasFieldOrPropertyWithValue("title", "Shared Title");
        assertThat(quiz).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("content", "Quiz Q");
    }

    @Test
    void createFrom_quizCommand_uuidDistinctFromCourseUuid() {
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
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(List.of(result, course))
                .extracting("uuid")
                .doesNotContainNull()
                .doesNotHaveDuplicates();
    }

    @Test
    void createFrom_quizCommand_doesNotHaveContentField() {
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
                .text("Text that should not appear")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result.getClass().getDeclaredFields())
                .extracting("name")
                .contains("questions")
                .doesNotContain("content");
    }

    @Test
    void createFrom_quizCommand_unicodeQuestionContent_preserved() {
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
                        new CreateQuestionCommand("日本語の質問"),
                        new CreateQuestionCommand("Вопрос αβγ \uD83D\uDCDA")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(2)
                .extracting("content")
                .containsExactly("日本語の質問", "Вопрос αβγ \uD83D\uDCDA");
    }

    @Test
    void createFrom_lectureCommand_idIsNullBeforePersistence() {
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
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result).hasFieldOrPropertyWithValue("id", null);
    }

    @Test
    void createFrom_quizCommand_idIsNullBeforePersistence() {
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
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).hasFieldOrPropertyWithValue("id", null);
    }

    @Test
    void createFrom_quizCommand_whitespaceOnlyQuestionContent_preserved() {
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
                .questions(List.of(new CreateQuestionCommand("   \t\n  ")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("content", "   \t\n  ");
    }

    @Test
    void createFrom_quizCommand_mixedNullAndPopulatedQuestionContent_allPreserved() {
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
                        new CreateQuestionCommand("Valid question"),
                        new CreateQuestionCommand(null),
                        new CreateQuestionCommand(""),
                        new CreateQuestionCommand("Another valid")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(4)
                .extracting("content")
                .containsExactly("Valid question", null, "", "Another valid");
    }

    @Test
    void createFrom_quizCommand_allQuestionsHaveNullIdBeforePersistence() {
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
                        assertThat(question).hasFieldOrPropertyWithValue("id", null));
    }

    @Test
    void createFrom_lectureCommandViaDirectConstructor_correctFieldMapping() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateLectureCommand lectureCommand = new CreateLectureCommand(
                "Direct Title", "Direct Desc", 42, "Direct Content");

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", "Direct Title")
                .hasFieldOrPropertyWithValue("description", "Direct Desc")
                .hasFieldOrPropertyWithValue("serialNumber", 42)
                .hasFieldOrPropertyWithValue("content", "Direct Content");
        assertThat(result).extracting("course").isSameAs(course);
    }

    @Test
    void createFrom_quizCommandViaDirectConstructor_correctFieldMapping() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = courseFactory.createFrom(createCourseCommand);

        final CreateQuizCommand quizCommand = new CreateQuizCommand(
                List.of(new CreateQuestionCommand("DQ1"), new CreateQuestionCommand("DQ2")),
                "Direct Quiz", "Direct Quiz Desc", 88, "Direct Text");

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", "Direct Quiz")
                .hasFieldOrPropertyWithValue("description", "Direct Quiz Desc")
                .hasFieldOrPropertyWithValue("serialNumber", 88);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .hasSize(2)
                .extracting("content")
                .containsExactly("DQ1", "DQ2");
        assertThat(result).extracting("course").isSameAs(course);
    }

    @Test
    void createFrom_quizCommand_questionEntityDoesNotHaveUuidField() {
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
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions")
                .asInstanceOf(LIST)
                .first()
                .satisfies(question ->
                        assertThat(question.getClass().getDeclaredFields())
                                .extracting("name")
                                .contains("id", "content", "quiz")
                                .doesNotContain("uuid"));
    }

}
