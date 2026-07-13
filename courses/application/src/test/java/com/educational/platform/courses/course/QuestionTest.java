package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuestionTest {

    @Test
    void createQuestion_validArguments_questionCreated() {
        // given
        final Quiz quiz = new Quiz();

        // when
        final Question question = new Question("what is java?", quiz);

        // then
        assertThat(question)
                .hasFieldOrPropertyWithValue("content", "what is java?")
                .hasFieldOrPropertyWithValue("quiz", quiz);
    }
}
