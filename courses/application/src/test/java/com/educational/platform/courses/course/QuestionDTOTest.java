package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuestionDTOTest {

    @Test
    void constructor_validContent_dtoCreated() {
        // when
        final QuestionDTO sut = new QuestionDTO("What is DDD?");

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", "What is DDD?");
    }

    @Test
    void constructor_emptyContent_dtoCreated() {
        // when
        final QuestionDTO sut = new QuestionDTO("");

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", "");
    }

    @Test
    void constructor_nullContent_dtoCreated() {
        // when
        final QuestionDTO sut = new QuestionDTO(null);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", null);
    }
}
