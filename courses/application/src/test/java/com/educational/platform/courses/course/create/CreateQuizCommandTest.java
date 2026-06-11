package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateQuizCommandTest {

    @Test
    void builder_allFieldsSet_commandCreated() {
        // given
        final List<CreateQuestionCommand> questions = List.of(
                new CreateQuestionCommand("What is Java?"),
                new CreateQuestionCommand("What is Spring?")
        );

        // when
        final CreateQuizCommand sut = CreateQuizCommand.builder()
                .title("Quiz Title")
                .description("Quiz Description")
                .serialNumber(2)
                .text("Quiz instructions")
                .questions(questions)
                .build();

        // then
        assertThat(sut.getTitle()).isEqualTo("Quiz Title");
        assertThat(sut.getDescription()).isEqualTo("Quiz Description");
        assertThat(sut.getSerialNumber()).isEqualTo(2);
        assertThat(sut.getText()).isEqualTo("Quiz instructions");
        assertThat(sut.getQuestions()).hasSize(2);
    }

    @Test
    void constructor_allFieldsPopulated() {
        // given
        final List<CreateQuestionCommand> questions = List.of(new CreateQuestionCommand("Q1"));

        // when
        final CreateQuizCommand sut = new CreateQuizCommand(questions, "Title", "Desc", 1, "Text");

        // then
        assertThat(sut.getTitle()).isEqualTo("Title");
        assertThat(sut.getDescription()).isEqualTo("Desc");
        assertThat(sut.getSerialNumber()).isEqualTo(1);
        assertThat(sut.getText()).isEqualTo("Text");
        assertThat(sut.getQuestions()).hasSize(1);
    }

    @Test
    void getQuestions_returnsQuestionsFromBuilder() {
        // given
        final CreateQuestionCommand q1 = new CreateQuestionCommand("Question 1");
        final CreateQuestionCommand q2 = new CreateQuestionCommand("Question 2");

        // when
        final CreateQuizCommand sut = CreateQuizCommand.builder()
                .title("T")
                .description("D")
                .serialNumber(1)
                .text("Txt")
                .questions(List.of(q1, q2))
                .build();

        // then
        assertThat(sut.getQuestions()).containsExactly(q1, q2);
    }

    @Test
    void builder_nullQuestions_storedAsNull() {
        // when
        final CreateQuizCommand sut = CreateQuizCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(1)
                .text("Text")
                .questions(null)
                .build();

        // then
        assertThat(sut.getQuestions()).isNull();
    }

    @Test
    void builder_emptyQuestions_storedAsEmptyList() {
        // when
        final CreateQuizCommand sut = CreateQuizCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(1)
                .text("Text")
                .questions(List.of())
                .build();

        // then
        assertThat(sut.getQuestions()).isEmpty();
    }

    @Test
    void extendsCreateCurriculumItemCommand() {
        // then
        assertThat(CreateCurriculumItemCommand.class).isAssignableFrom(CreateQuizCommand.class);
    }

    @Test
    void builder_singleQuestion_storedCorrectly() {
        // given
        final CreateQuestionCommand question = new CreateQuestionCommand("Only question");

        // when
        final CreateQuizCommand sut = CreateQuizCommand.builder()
                .title("Quiz")
                .description("D")
                .serialNumber(1)
                .text("T")
                .questions(List.of(question))
                .build();

        // then
        assertThat(sut.getQuestions()).hasSize(1);
        assertThat(sut.getQuestions().getFirst().content()).isEqualTo("Only question");
    }
}
