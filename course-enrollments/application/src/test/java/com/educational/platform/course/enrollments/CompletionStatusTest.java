package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CompletionStatusTest {

	@Test
	void toDTO_inProgress_mapsToInProgressDTO() {
		// when
		final CompletionStatusDTO result = CompletionStatus.IN_PROGRESS.toDTO();

		// then
		assertThat(result).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
	}

	@Test
	void toDTO_completed_mapsToCompletedDTO() {
		// when
		final CompletionStatusDTO result = CompletionStatus.COMPLETED.toDTO();

		// then
		assertThat(result).isEqualTo(CompletionStatusDTO.COMPLETED);
	}

	@Test
	void valueOf_invalidName_throwsIllegalArgumentException() {
		// when / then
		assertThatThrownBy(() -> CompletionStatus.valueOf("INVALID"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void values_containsExpectedStatuses() {
		// when
		final CompletionStatus[] values = CompletionStatus.values();

		// then
		assertThat(values).containsExactlyInAnyOrder(
				CompletionStatus.IN_PROGRESS,
				CompletionStatus.COMPLETED
		);
	}

	@Test
	void toDTO_allValues_mapToNonNullDTO() {
		for (CompletionStatus status : CompletionStatus.values()) {
			// when
			final CompletionStatusDTO result = status.toDTO();

			// then
			assertThat(result).isNotNull();
		}
	}
}
