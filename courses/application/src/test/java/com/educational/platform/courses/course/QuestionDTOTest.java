package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuestionDTOTest {

    @Test
    void constructor_setsContent() {
        // when
        final QuestionDTO dto = new QuestionDTO("What is Java?");

        // then
        assertThat(dto.content).isEqualTo("What is Java?");
    }

    @Test
    void constructor_nullContent_allowed() {
        // when
        final QuestionDTO dto = new QuestionDTO(null);

        // then
        assertThat(dto.content).isNull();
    }

    @Test
    void constructor_emptyContent_allowed() {
        // when
        final QuestionDTO dto = new QuestionDTO("");

        // then
        assertThat(dto.content).isEmpty();
    }
}
