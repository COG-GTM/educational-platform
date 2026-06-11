package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link CreateCurriculumItemCommand} abstract base class through its concrete subclasses,
 * focusing on inheritance, null handling, and builder patterns.
 */
public class CurriculumItemCommandHierarchyTest {

    // --- CreateLectureCommand null handling ---

    @Test
    void createLectureCommand_nullTitle_allowed() {
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title(null)
                .description("desc")
                .serialNumber(1)
                .text("text")
                .build();

        assertThat(command.getTitle()).isNull();
    }

    @Test
    void createLectureCommand_nullDescription_allowed() {
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("title")
                .description(null)
                .serialNumber(1)
                .text("text")
                .build();

        assertThat(command.getDescription()).isNull();
    }

    @Test
    void createLectureCommand_nullSerialNumber_allowed() {
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("title")
                .description("desc")
                .serialNumber(null)
                .text("text")
                .build();

        assertThat(command.getSerialNumber()).isNull();
    }

    @Test
    void createLectureCommand_nullText_allowed() {
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("title")
                .description("desc")
                .serialNumber(1)
                .text(null)
                .build();

        assertThat(command.getText()).isNull();
    }

    @Test
    void createLectureCommand_isInstanceOfCurriculumItemCommand() {
        final CreateLectureCommand command = new CreateLectureCommand("t", "d", 1, "txt");
        assertThat(command).isInstanceOf(CreateCurriculumItemCommand.class);
    }

    // --- CreateQuizCommand null handling ---

    @Test
    void createQuizCommand_nullQuestions_allowed() {
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("title")
                .description("desc")
                .serialNumber(1)
                .text("text")
                .questions(null)
                .build();

        assertThat(command.getQuestions()).isNull();
    }

    @Test
    void createQuizCommand_emptyQuestions_allowed() {
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("title")
                .description("desc")
                .serialNumber(1)
                .text("text")
                .questions(List.of())
                .build();

        assertThat(command.getQuestions()).isEmpty();
    }

    @Test
    void createQuizCommand_isInstanceOfCurriculumItemCommand() {
        final CreateQuizCommand command = new CreateQuizCommand(List.of(), "t", "d", 1, "txt");
        assertThat(command).isInstanceOf(CreateCurriculumItemCommand.class);
    }

    @Test
    void createQuizCommand_multipleQuestions_allPreserved() {
        final CreateQuestionCommand q1 = new CreateQuestionCommand("Q1");
        final CreateQuestionCommand q2 = new CreateQuestionCommand("Q2");
        final CreateQuestionCommand q3 = new CreateQuestionCommand("Q3");

        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Quiz")
                .description("desc")
                .serialNumber(1)
                .text("intro")
                .questions(List.of(q1, q2, q3))
                .build();

        assertThat(command.getQuestions()).containsExactly(q1, q2, q3);
    }

    // --- CreateQuestionCommand validation ---

    @Test
    void createQuestionCommand_nullContent_allowed() {
        final CreateQuestionCommand command = new CreateQuestionCommand(null);
        assertThat(command.content()).isNull();
    }

    @Test
    void createQuestionCommand_equalInstances() {
        assertThat(new CreateQuestionCommand("content"))
                .isEqualTo(new CreateQuestionCommand("content"));
    }

    @Test
    void createQuestionCommand_differentContent_notEqual() {
        assertThat(new CreateQuestionCommand("Q1"))
                .isNotEqualTo(new CreateQuestionCommand("Q2"));
    }

    // --- Builder chaining ---

    @Test
    void createLectureCommand_builderChaining_returnsSameBuilder() {
        final CreateLectureCommand.CreateLectureCommandBuilder builder = CreateLectureCommand.builder();
        assertThat(builder.title("t")).isSameAs(builder);
        assertThat(builder.description("d")).isSameAs(builder);
        assertThat(builder.serialNumber(1)).isSameAs(builder);
        assertThat(builder.text("txt")).isSameAs(builder);
    }

    @Test
    void createQuizCommand_builderChaining_returnsSameBuilder() {
        final CreateQuizCommand.CreateQuizCommandBuilder builder = CreateQuizCommand.builder();
        assertThat(builder.title("t")).isSameAs(builder);
        assertThat(builder.description("d")).isSameAs(builder);
        assertThat(builder.serialNumber(1)).isSameAs(builder);
        assertThat(builder.text("txt")).isSameAs(builder);
        assertThat(builder.questions(List.of())).isSameAs(builder);
    }
}
