package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LectureTypeTest {

    @Test
    void values_singleTextValue() {
        // when
        final LectureType[] values = LectureType.values();

        // then
        assertThat(values).containsExactly(LectureType.TEXT);
    }

    @Test
    void valueOf_text_correctValue() {
        // when
        final LectureType result = LectureType.valueOf("TEXT");

        // then
        assertThat(result).isEqualTo(LectureType.TEXT);
    }

    @Test
    void valueOf_invalidName_illegalArgumentException() {
        // when / then
        assertThatThrownBy(() -> LectureType.valueOf("VIDEO"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
