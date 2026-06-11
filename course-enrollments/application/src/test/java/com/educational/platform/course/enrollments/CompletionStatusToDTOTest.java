package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link CompletionStatus#toDTO()} mapping for all enum values.
 */
public class CompletionStatusToDTOTest {

    @Test
    void inProgress_mapsToInProgressDTO() {
        assertThat(CompletionStatus.IN_PROGRESS.toDTO()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void completed_mapsToCompletedDTO() {
        assertThat(CompletionStatus.COMPLETED.toDTO()).isEqualTo(CompletionStatusDTO.COMPLETED);
    }
}
