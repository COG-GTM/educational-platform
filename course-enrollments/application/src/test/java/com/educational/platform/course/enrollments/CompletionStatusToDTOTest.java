package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link CompletionStatus#toDTO()} mapping for all enum values.
 */
public class CompletionStatusToDTOTest {

    @Test
    void toDTO_inProgress_mapsToInProgressDTO() {
        assertThat(CompletionStatus.IN_PROGRESS.toDTO()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void toDTO_completed_mapsToCompletedDTO() {
        assertThat(CompletionStatus.COMPLETED.toDTO()).isEqualTo(CompletionStatusDTO.COMPLETED);
    }

    @Test
    void allValuesHaveDTO_noNullMappings() {
        for (CompletionStatus status : CompletionStatus.values()) {
            assertThat(status.toDTO())
                    .as("CompletionStatus.%s should map to a non-null DTO", status.name())
                    .isNotNull();
        }
    }
}
