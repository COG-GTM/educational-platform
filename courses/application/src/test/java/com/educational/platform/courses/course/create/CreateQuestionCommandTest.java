package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateQuestionCommandTest {

    @Test
    void constructor_validContent_contentStored() {
        // when
        final CreateQuestionCommand sut = new CreateQuestionCommand("What is polymorphism?");

        // then
        assertThat(sut.content()).isEqualTo("What is polymorphism?");
    }

    @Test
    void constructor_emptyContent_contentStored() {
        // when
        final CreateQuestionCommand sut = new CreateQuestionCommand("");

        // then
        assertThat(sut.content()).isEmpty();
    }

    @Test
    void constructor_nullContent_contentIsNull() {
        // when
        final CreateQuestionCommand sut = new CreateQuestionCommand(null);

        // then
        assertThat(sut.content()).isNull();
    }

    @Test
    void equality_sameContent_equal() {
        // given
        final CreateQuestionCommand cmd1 = new CreateQuestionCommand("question");
        final CreateQuestionCommand cmd2 = new CreateQuestionCommand("question");

        // then
        assertThat(cmd1).isEqualTo(cmd2);
        assertThat(cmd1.hashCode()).isEqualTo(cmd2.hashCode());
    }

    @Test
    void equality_differentContent_notEqual() {
        // given
        final CreateQuestionCommand cmd1 = new CreateQuestionCommand("question1");
        final CreateQuestionCommand cmd2 = new CreateQuestionCommand("question2");

        // then
        assertThat(cmd1).isNotEqualTo(cmd2);
    }
}
