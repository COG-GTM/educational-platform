package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CompletionStatusTest {

    @Test
    void toDTO_inProgress_returnsInProgressDTO() {
        // given
        final CompletionStatus sut = CompletionStatus.IN_PROGRESS;

        // when
        final CompletionStatusDTO result = sut.toDTO();

        // then
        assertThat(result).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void toDTO_completed_returnsCompletedDTO() {
        // given
        final CompletionStatus sut = CompletionStatus.COMPLETED;

        // when
        final CompletionStatusDTO result = sut.toDTO();

        // then
        assertThat(result).isEqualTo(CompletionStatusDTO.COMPLETED);
    }
}
