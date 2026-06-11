package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QuizTest {

    private static final Integer TEACHER_ID = 15;

    private Course createCourse() {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("course")
                .description("desc")
                .build();
        return new Course(command, TEACHER_ID);
    }

    @Test
    void constructor_allFields_storedCorrectly() {
        // given
        final Course course = createCourse();
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Quiz Title")
                .description("Quiz Description")
                .serialNumber(2)
                .text("Quiz Text")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final Quiz sut = new Quiz(command, 2, course);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("title", "Quiz Title")
                .hasFieldOrPropertyWithValue("description", "Quiz Description")
                .hasFieldOrPropertyWithValue("serialNumber", 2)
                .hasFieldOrPropertyWithValue("course", course);
    }

    @Test
    void constructor_singleQuestion_questionCreated() {
        // given
        final Course course = createCourse();
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("text")
                .questions(List.of(new CreateQuestionCommand("What is 2+2?")))
                .build();

        // when
        final Quiz sut = new Quiz(command, 1, course);

        // then
        final List<?> questions = (List<?>) org.springframework.test.util.ReflectionTestUtils.getField(sut, "questions");
        assertThat(questions).hasSize(1);
        assertThat(questions.get(0))
                .isInstanceOf(Question.class)
                .hasFieldOrPropertyWithValue("content", "What is 2+2?");
    }

    @Test
    void constructor_multipleQuestions_allQuestionsCreated() {
        // given
        final Course course = createCourse();
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("text")
                .questions(List.of(
                        new CreateQuestionCommand("Q1"),
                        new CreateQuestionCommand("Q2"),
                        new CreateQuestionCommand("Q3")
                ))
                .build();

        // when
        final Quiz sut = new Quiz(command, 1, course);

        // then
        final List<?> questions = (List<?>) org.springframework.test.util.ReflectionTestUtils.getField(sut, "questions");
        assertThat(questions).hasSize(3);
    }

    @Test
    void constructor_emptyQuestionsList_emptyQuestionsCreated() {
        // given
        final Course course = createCourse();
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("text")
                .questions(Collections.emptyList())
                .build();

        // when
        final Quiz sut = new Quiz(command, 1, course);

        // then
        final List<?> questions = (List<?>) org.springframework.test.util.ReflectionTestUtils.getField(sut, "questions");
        assertThat(questions).isEmpty();
    }

    @Test
    void constructor_questionsLinkedBackToQuiz() {
        // given
        final Course course = createCourse();
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("text")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final Quiz sut = new Quiz(command, 1, course);

        // then
        final List<?> questions = (List<?>) org.springframework.test.util.ReflectionTestUtils.getField(sut, "questions");
        assertThat(questions.get(0)).hasFieldOrPropertyWithValue("quiz", sut);
    }

    @Test
    void constructor_serialNumberFromParameter_notFromCommand() {
        // given
        final Course course = createCourse();
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(99)
                .text("text")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when — serialNumber parameter (7) overrides command's serialNumber (99)
        final Quiz sut = new Quiz(command, 7, course);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("serialNumber", 7);
    }

    @Test
    void constructor_uuidGenerated() {
        // given
        final Course course = createCourse();
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("text")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final Quiz sut = new Quiz(command, 1, course);

        // then
        assertThat(sut).extracting("uuid").isNotNull();
    }

    @Test
    void isCurriculumItem() {
        // then
        assertThat(CurriculumItem.class).isAssignableFrom(Quiz.class);
    }

    @Test
    void constructor_nullTitleAndDescription_storedAsNull() {
        // given
        final Course course = createCourse();
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title(null)
                .description(null)
                .serialNumber(1)
                .text("text")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        // when
        final Quiz sut = new Quiz(command, 1, course);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("title", null)
                .hasFieldOrPropertyWithValue("description", null);
    }
}
