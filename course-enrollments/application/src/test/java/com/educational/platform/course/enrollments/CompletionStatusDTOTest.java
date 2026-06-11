package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CompletionStatusDTOTest {

	@Test
	void values_containsExpectedStatuses() {
		// when
		final CompletionStatusDTO[] values = CompletionStatusDTO.values();

		// then
		assertThat(values).containsExactlyInAnyOrder(
				CompletionStatusDTO.IN_PROGRESS,
				CompletionStatusDTO.COMPLETED
		);
	}

	@Test
	void valueOf_inProgress_returnsInProgress() {
		// when
		final CompletionStatusDTO status = CompletionStatusDTO.valueOf("IN_PROGRESS");

		// then
		assertThat(status).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
	}

	@Test
	void valueOf_completed_returnsCompleted() {
		// when
		final CompletionStatusDTO status = CompletionStatusDTO.valueOf("COMPLETED");

		// then
		assertThat(status).isEqualTo(CompletionStatusDTO.COMPLETED);
	}
}
