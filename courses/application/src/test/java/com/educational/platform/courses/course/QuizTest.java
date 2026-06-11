package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class QuizTest {

    @Test
    void constructor_initializesFieldsFromCommand() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("course").description("desc").build(), 1);
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("First quiz")
                .serialNumber(2)
                .text("Quiz intro")
                .questions(List.of(
                        new CreateQuestionCommand("What is Java?"),
                        new CreateQuestionCommand("What is OOP?")))
                .build();

        // when
        final Quiz quiz = new Quiz(command, command.getSerialNumber(), course);

        // then
        assertThat(quiz)
                .hasFieldOrPropertyWithValue("title", "Quiz 1")
                .hasFieldOrPropertyWithValue("description", "First quiz")
                .hasFieldOrPropertyWithValue("serialNumber", 2)
                .hasFieldOrPropertyWithValue("course", course);
        assertThat(quiz).hasFieldOrProperty("uuid");
    }

    @Test
    void constructor_createsQuestionsFromCommand() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("c").description("d").build(), 1);
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Quiz")
                .description("desc")
                .serialNumber(1)
                .text("text")
                .questions(List.of(
                        new CreateQuestionCommand("Q1"),
                        new CreateQuestionCommand("Q2"),
                        new CreateQuestionCommand("Q3")))
                .build();

        // when
        final Quiz quiz = new Quiz(command, 1, course);

        // then
        @SuppressWarnings("unchecked")
        final List<Question> questions = (List<Question>) org.springframework.test.util.ReflectionTestUtils.getField(quiz, "questions");
        assertThat(questions).hasSize(3);
    }

    @Test
    void constructor_singleQuestion_createsOneQuestion() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("c").description("d").build(), 1);
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Quiz")
                .description("desc")
                .serialNumber(1)
                .text("text")
                .questions(List.of(new CreateQuestionCommand("Only question")))
                .build();

        // when
        final Quiz quiz = new Quiz(command, 1, course);

        // then
        @SuppressWarnings("unchecked")
        final List<Question> questions = (List<Question>) org.springframework.test.util.ReflectionTestUtils.getField(quiz, "questions");
        assertThat(questions).hasSize(1);
        assertThat(questions.getFirst()).hasFieldOrPropertyWithValue("content", "Only question");
    }

    @Test
    void constructor_isCurriculumItem() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("c").description("d").build(), 1);
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("t").description("d").serialNumber(1).text("x")
                .questions(List.of(new CreateQuestionCommand("Q")))
                .build();

        // when
        final CurriculumItem item = new Quiz(command, 1, course);

        // then
        assertThat(item).isInstanceOf(Quiz.class);
        assertThat(item).isInstanceOf(CurriculumItem.class);
    }
}
