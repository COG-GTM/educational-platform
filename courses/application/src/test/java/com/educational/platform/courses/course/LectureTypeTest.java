package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LectureTypeTest {

    @Test
    void values_containsOnlyText() {
        // then
        assertThat(LectureType.values()).containsExactly(LectureType.TEXT);
    }

    @Test
    void values_hasOneValue() {
        // then
        assertThat(LectureType.values()).hasSize(1);
    }

    @Test
    void valueOf_TEXT_returnsCorrectEnum() {
        // then
        assertThat(LectureType.valueOf("TEXT")).isEqualTo(LectureType.TEXT);
    }

    @Test
    void valueOf_invalidName_throwsIllegalArgumentException() {
        // then
        assertThatThrownBy(() -> LectureType.valueOf("VIDEO"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void name_returnsCorrectString() {
        // then
        assertThat(LectureType.TEXT.name()).isEqualTo("TEXT");
    }

    @Test
    void ordinal_textIsZero() {
        // then
        assertThat(LectureType.TEXT.ordinal()).isEqualTo(0);
    }
}
