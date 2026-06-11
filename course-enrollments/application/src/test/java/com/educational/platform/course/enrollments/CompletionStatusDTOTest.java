package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CompletionStatusDTOTest {

    @Test
    void values_allStatusesExist() {
        // given
        final CompletionStatusDTO[] values = CompletionStatusDTO.values();

        // when / then
        assertThat(values).containsExactlyInAnyOrder(
                CompletionStatusDTO.IN_PROGRESS,
                CompletionStatusDTO.COMPLETED
        );
    }

    @Test
    void valueOf_inProgress_correctValue() {
        // when
        final CompletionStatusDTO status = CompletionStatusDTO.valueOf("IN_PROGRESS");

        // then
        assertThat(status).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void valueOf_completed_correctValue() {
        // when
        final CompletionStatusDTO status = CompletionStatusDTO.valueOf("COMPLETED");

        // then
        assertThat(status).isEqualTo(CompletionStatusDTO.COMPLETED);
    }
}
