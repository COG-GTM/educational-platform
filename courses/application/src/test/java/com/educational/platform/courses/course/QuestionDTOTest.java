package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuestionDTOTest {

    @Test
    void constructor_mapsContent() {
        // given - a question DTO is the read-model representation of a quiz question; its only
        // state is the question content carried over from the constructor argument
        final QuestionDTO dto = new QuestionDTO("What is a bounded context?");

        // then
        assertThat(dto).hasFieldOrPropertyWithValue("content", "What is a bounded context?");
    }

    @Test
    void constructor_nullContent_contentIsNull() {
        // given - the constructor performs a plain assignment without validation, so a null content
        // is stored as-is rather than rejected
        final QuestionDTO dto = new QuestionDTO(null);

        // then
        assertThat(dto).hasFieldOrPropertyWithValue("content", null);
    }
}
