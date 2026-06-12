package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateQuizCommandTest {

    @Test
    void builder_allFieldsSet_commandCreatedWithCorrectValues() {
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
                .text("Quiz text")
                .questions(questions)
                .build();

        // then
        assertThat(sut.getTitle()).isEqualTo("Quiz Title");
        assertThat(sut.getDescription()).isEqualTo("Quiz Description");
        assertThat(sut.getSerialNumber()).isEqualTo(2);
        assertThat(sut.getText()).isEqualTo("Quiz text");
        assertThat(sut.getQuestions()).hasSize(2);
    }

    @Test
    void builder_nullFields_commandCreatedWithNulls() {
        // when
        final CreateQuizCommand sut = CreateQuizCommand.builder().build();

        // then
        assertThat(sut.getTitle()).isNull();
        assertThat(sut.getDescription()).isNull();
        assertThat(sut.getSerialNumber()).isNull();
        assertThat(sut.getText()).isNull();
        assertThat(sut.getQuestions()).isNull();
    }

    @Test
    void builder_emptyQuestionsList_commandCreatedWithEmptyList() {
        // when
        final CreateQuizCommand sut = CreateQuizCommand.builder()
                .title("Quiz")
                .questions(List.of())
                .build();

        // then
        assertThat(sut.getQuestions()).isEmpty();
    }

    @Test
    void constructor_directInstantiation_fieldsAccessible() {
        // given
        final List<CreateQuestionCommand> questions = List.of(new CreateQuestionCommand("q1"));

        // when
        final CreateQuizCommand sut = new CreateQuizCommand(questions, "title", "desc", 3, "text");

        // then
        assertThat(sut.getTitle()).isEqualTo("title");
        assertThat(sut.getDescription()).isEqualTo("desc");
        assertThat(sut.getSerialNumber()).isEqualTo(3);
        assertThat(sut.getText()).isEqualTo("text");
        assertThat(sut.getQuestions()).hasSize(1);
        assertThat(sut.getQuestions().getFirst().content()).isEqualTo("q1");
    }

    @Test
    void builder_isSubclassOfCreateCurriculumItemCommand() {
        // when
        final CreateQuizCommand sut = CreateQuizCommand.builder()
                .title("title")
                .build();

        // then
        assertThat(sut).isInstanceOf(CreateCurriculumItemCommand.class);
    }
}
