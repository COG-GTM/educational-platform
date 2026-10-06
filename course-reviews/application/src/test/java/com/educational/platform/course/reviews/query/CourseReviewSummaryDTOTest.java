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
		assertThat(summary.ratingCounts()).containsExactlyInAnyOrderEntriesOf(Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
	}

	@Test
	void from_fractionalRatings_roundedToNearestStarAndClamped() {
		var summary = CourseReviewSummaryDTO.from(List.of(4.4, 4.5, 0.2, 5.0, 2.49));

		assertThat(summary.totalReviews()).isEqualTo(5);
		assertThat(summary.averageRating()).isCloseTo(3.318, within(0.001));
		assertThat(summary.ratingCounts()).containsEntry(1, 1L).containsEntry(2, 1L).containsEntry(3, 0L).containsEntry(4, 1L).containsEntry(5, 2L);
	}

	@Test
	void from_halfStarRatings_roundedHalfUp() {
		var summary = CourseReviewSummaryDTO.from(List.of(1.5, 2.5, 3.5));

		assertThat(summary.ratingCounts()).containsExactlyInAnyOrderEntriesOf(Map.of(1, 0L, 2, 1L, 3, 1L, 4, 1L, 5, 0L));
	}

	@Test
	void from_ratingAboveMaxStar_clampedToMaxStar() {
		var summary = CourseReviewSummaryDTO.from(List.of(7.0, 5.4));

		assertThat(summary.ratingCounts()).containsEntry(5, 2L);
		assertThat(summary.averageRating()).isCloseTo(6.2, within(0.001));
	}

	@Test
	void from_singleRating_averageIsThatRating() {
		var summary = CourseReviewSummaryDTO.from(List.of(3.7));

		assertThat(summary.totalReviews()).isEqualTo(1);
		assertThat(summary.averageRating()).isEqualTo(3.7);
		assertThat(summary.ratingCounts()).containsExactlyInAnyOrderEntriesOf(Map.of(1, 0L, 2, 0L, 3, 0L, 4, 1L, 5, 0L));
	}

	@Test
	void from_anyRatings_countsSumToTotalAndKeysAscending() {
		var summary = CourseReviewSummaryDTO.from(List.of(1.0, 1.0, 2.0, 4.9, 5.0, 3.3));

		assertThat(summary.ratingCounts().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(summary.totalReviews());
		assertThat(summary.ratingCounts().keySet()).containsExactly(1, 2, 3, 4, 5);
	}
}
