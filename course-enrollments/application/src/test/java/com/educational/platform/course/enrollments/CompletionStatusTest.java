package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CompletionStatusTest {

    @Test
    void toDTO_inProgress_mappedToInProgressDTO() {
        // when
        final CompletionStatusDTO dto = CompletionStatus.IN_PROGRESS.toDTO();

        // then
        assertThat(dto).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void toDTO_completed_mappedToCompletedDTO() {
        // when
        final CompletionStatusDTO dto = CompletionStatus.COMPLETED.toDTO();

        // then
        assertThat(dto).isEqualTo(CompletionStatusDTO.COMPLETED);
    }
}
