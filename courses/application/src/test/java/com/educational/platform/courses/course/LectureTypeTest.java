package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LectureTypeTest {

    @Test
    void values_containsOnlyText() {
        assertThat(LectureType.values()).containsExactly(LectureType.TEXT);
    }

    @Test
    void valueOf_text_returnsText() {
        assertThat(LectureType.valueOf("TEXT")).isEqualTo(LectureType.TEXT);
    }

    @Test
    void valueOf_invalid_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> LectureType.valueOf("VIDEO"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
