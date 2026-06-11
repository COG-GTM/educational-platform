package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link CreateQuizCommand} builder happy-path and direct constructor accessors.
 */
public class CreateQuizCommandBuilderTest {

    @Test
    void builder_allFieldsSet_accessorsReturnCorrectValues() {
        // given
        final CreateQuestionCommand q1 = new CreateQuestionCommand("What is 2+2?");
        final CreateQuestionCommand q2 = new CreateQuestionCommand("What is 3+3?");

        // when
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Math Quiz")
                .description("Basic arithmetic")
                .serialNumber(2)
                .text("Answer the following questions")
                .questions(List.of(q1, q2))
                .build();

        // then
        assertThat(command.getTitle()).isEqualTo("Math Quiz");
        assertThat(command.getDescription()).isEqualTo("Basic arithmetic");
        assertThat(command.getSerialNumber()).isEqualTo(2);
        assertThat(command.getText()).isEqualTo("Answer the following questions");
        assertThat(command.getQuestions()).containsExactly(q1, q2);
    }

    @Test
    void directConstructor_allFieldsSet_accessorsReturnCorrectValues() {
        // given
        final CreateQuestionCommand q = new CreateQuestionCommand("Q1");

        // when
        final CreateQuizCommand command = new CreateQuizCommand(List.of(q), "title", "desc", 3, "intro");

        // then
        assertThat(command.getTitle()).isEqualTo("title");
        assertThat(command.getDescription()).isEqualTo("desc");
        assertThat(command.getSerialNumber()).isEqualTo(3);
        assertThat(command.getText()).isEqualTo("intro");
        assertThat(command.getQuestions()).containsExactly(q);
    }

    @Test
    void builder_returnsNewBuilderEachTime() {
        // when
        final CreateQuizCommand.CreateQuizCommandBuilder first = CreateQuizCommand.builder();
        final CreateQuizCommand.CreateQuizCommandBuilder second = CreateQuizCommand.builder();

        // then
        assertThat(first).isNotSameAs(second);
    }

    @Test
    void builder_minimalFields_buildsSuccessfully() {
        // when
        final CreateQuizCommand command = CreateQuizCommand.builder().build();

        // then
        assertThat(command.getTitle()).isNull();
        assertThat(command.getDescription()).isNull();
        assertThat(command.getSerialNumber()).isNull();
        assertThat(command.getText()).isNull();
        assertThat(command.getQuestions()).isNull();
    }
}
