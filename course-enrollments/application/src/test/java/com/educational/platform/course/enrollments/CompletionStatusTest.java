package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CompletionStatusTest {

    @Test
    void toDTO_inProgress_mapsToInProgressDTO() {
        assertThat(CompletionStatus.IN_PROGRESS.toDTO()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void toDTO_completed_mapsToCompletedDTO() {
        assertThat(CompletionStatus.COMPLETED.toDTO()).isEqualTo(CompletionStatusDTO.COMPLETED);
    }

    @Test
    void completionStatus_hasExpectedValues() {
        assertThat(CompletionStatus.values()).containsExactly(CompletionStatus.IN_PROGRESS, CompletionStatus.COMPLETED);
    }

    @Test
    void completionStatusDTO_hasExpectedValues() {
        assertThat(CompletionStatusDTO.values()).containsExactly(CompletionStatusDTO.IN_PROGRESS, CompletionStatusDTO.COMPLETED);
    }
}
