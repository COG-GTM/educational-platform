package com.educational.platform.course.reviews.query;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CourseReviewSummaryDTOTest {

	@Test
	void from_noRatings_zeroesWithEveryStarLevel() {
		var summary = CourseReviewSummaryDTO.from(List.of());

		assertThat(summary.totalReviews()).isZero();
		assertThat(summary.averageRating()).isZero();
		assertThat(summary.ratingCounts()).containsExactlyEntriesOf(Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
	}

	@Test
	void from_fractionalRatings_roundedToNearestStarAndClamped() {
		var summary = CourseReviewSummaryDTO.from(List.of(4.4, 4.5, 0.2, 5.0, 2.49));

		assertThat(summary.totalReviews()).isEqualTo(5);
		assertThat(summary.averageRating()).isCloseTo(3.318, within(0.001));
		assertThat(summary.ratingCounts()).containsEntry(1, 1L).containsEntry(2, 1L).containsEntry(3, 0L).containsEntry(4, 1L).containsEntry(5, 2L);
	}
}
