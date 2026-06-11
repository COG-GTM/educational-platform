package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the {@link CompletionStatusDTO} enum values and valueOf.
 */
public class CompletionStatusDTOTest {

    @Test
    void containsExpectedValues() {
        assertThat(CompletionStatusDTO.values())
                .containsExactly(CompletionStatusDTO.IN_PROGRESS, CompletionStatusDTO.COMPLETED);
    }

    @Test
    void valueOf_inProgress() {
        assertThat(CompletionStatusDTO.valueOf("IN_PROGRESS")).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void valueOf_completed() {
        assertThat(CompletionStatusDTO.valueOf("COMPLETED")).isEqualTo(CompletionStatusDTO.COMPLETED);
    }

    @Test
    void valueOf_invalid_throwsException() {
        assertThatThrownBy(() -> CompletionStatusDTO.valueOf("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void valuesCount() {
        assertThat(CompletionStatusDTO.values()).hasSize(2);
    }
}
