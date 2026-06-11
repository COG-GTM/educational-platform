package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuestionDTOTest {

    @Test
    void constructor_contentPopulated() {
        // when
        final QuestionDTO sut = new QuestionDTO("What is Java?");

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", "What is Java?");
    }

    @Test
    void constructor_nullContent_storedAsNull() {
        // when
        final QuestionDTO sut = new QuestionDTO(null);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", null);
    }

    @Test
    void constructor_emptyContent_storedAsEmpty() {
        // when
        final QuestionDTO sut = new QuestionDTO("");

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", "");
    }

    @Test
    void constructor_unicodeContent_storedCorrectly() {
        // when
        final QuestionDTO sut = new QuestionDTO("日本語の質問");

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", "日本語の質問");
    }
}
