package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CompletionStatusTest {

    @Test
    void toDTO_inProgress_inProgressDTO() {
        // when
        final CompletionStatusDTO result = CompletionStatus.IN_PROGRESS.toDTO();

        // then
        assertThat(result).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void toDTO_completed_completedDTO() {
        // when
        final CompletionStatusDTO result = CompletionStatus.COMPLETED.toDTO();

        // then
        assertThat(result).isEqualTo(CompletionStatusDTO.COMPLETED);
    }
}
