package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class QuestionTest {

    private static final Integer TEACHER_ID = 15;

    private Quiz createQuiz() {
        final CreateCourseCommand courseCommand = CreateCourseCommand.builder()
                .name("course")
                .description("desc")
                .build();
        final Course course = new Course(courseCommand, TEACHER_ID);
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(1)
                .text("text")
                .questions(List.of(new CreateQuestionCommand("placeholder")))
                .build();
        return new Quiz(quizCommand, 1, course);
    }

    @Test
    void constructor_contentStoredCorrectly() {
        // given
        final Quiz quiz = createQuiz();

        // when
        final Question sut = new Question("What is Java?", quiz);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", "What is Java?");
    }

    @Test
    void constructor_quizReferenceStored() {
        // given
        final Quiz quiz = createQuiz();

        // when
        final Question sut = new Question("Q1", quiz);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("quiz", quiz);
    }

    @Test
    void constructor_nullContent_storedAsNull() {
        // given
        final Quiz quiz = createQuiz();

        // when
        final Question sut = new Question(null, quiz);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", null);
    }

    @Test
    void constructor_emptyContent_storedAsEmpty() {
        // given
        final Quiz quiz = createQuiz();

        // when
        final Question sut = new Question("", quiz);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", "");
    }

    @Test
    void constructor_idIsNullBeforePersistence() {
        // given
        final Quiz quiz = createQuiz();

        // when
        final Question sut = new Question("Q1", quiz);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("id", null);
    }

    @Test
    void constructor_unicodeContent_preserved() {
        // given
        final Quiz quiz = createQuiz();

        // when
        final Question sut = new Question("¿Cuál es la respuesta? 日本語テスト", quiz);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", "¿Cuál es la respuesta? 日本語テスト");
    }

    @Test
    void constructor_longContent_storedCorrectly() {
        // given
        final Quiz quiz = createQuiz();
        final String longContent = "A".repeat(10000);

        // when
        final Question sut = new Question(longContent, quiz);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", longContent);
    }
}
