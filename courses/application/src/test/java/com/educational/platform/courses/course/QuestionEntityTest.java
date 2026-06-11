package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Direct unit tests for the {@link Question} entity constructor. The entity
 * is normally created through {@link Quiz}, but these tests verify the
 * constructor assigns fields correctly.
 */
public class QuestionEntityTest {

    private Quiz createQuiz() {
        final Course course = new Course(
                CreateCourseCommand.builder().name("c").description("d").build(), 1);
        final CreateQuizCommand cmd = CreateQuizCommand.builder()
                .title("Quiz")
                .description("desc")
                .serialNumber(1)
                .text("text")
                .questions(List.of(new CreateQuestionCommand("placeholder")))
                .build();
        return new Quiz(cmd, 1, course);
    }

    @Test
    void constructor_setsContent() {
        // given
        final Quiz quiz = createQuiz();

        // when
        final Question question = new Question("What is polymorphism?", quiz);

        // then
        assertThat(question).hasFieldOrPropertyWithValue("content", "What is polymorphism?");
    }

    @Test
    void constructor_setsQuizReference() {
        // given
        final Quiz quiz = createQuiz();

        // when
        final Question question = new Question("content", quiz);

        // then
        assertThat(question).hasFieldOrPropertyWithValue("quiz", quiz);
    }

    @Test
    void constructor_nullContent_allowed() {
        // given
        final Quiz quiz = createQuiz();

        // when
        final Question question = new Question(null, quiz);

        // then
        assertThat(question).hasFieldOrPropertyWithValue("content", null);
    }

    @Test
    void constructor_emptyContent_allowed() {
        // given
        final Quiz quiz = createQuiz();

        // when
        final Question question = new Question("", quiz);

        // then
        assertThat(question).hasFieldOrPropertyWithValue("content", "");
    }

    @Test
    void quizCreation_questionsStoreContentFromCommands() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("c").description("d").build(), 1);
        final CreateQuizCommand cmd = CreateQuizCommand.builder()
                .title("Quiz")
                .description("desc")
                .serialNumber(1)
                .text("text")
                .questions(List.of(
                        new CreateQuestionCommand("Q1"),
                        new CreateQuestionCommand("Q2")))
                .build();

        // when
        final Quiz quiz = new Quiz(cmd, 1, course);

        // then
        @SuppressWarnings("unchecked")
        final List<Question> questions = (List<Question>) ReflectionTestUtils.getField(quiz, "questions");
        assertThat(questions).hasSize(2);
        assertThat(questions.get(0)).hasFieldOrPropertyWithValue("content", "Q1");
        assertThat(questions.get(1)).hasFieldOrPropertyWithValue("content", "Q2");
    }
}
