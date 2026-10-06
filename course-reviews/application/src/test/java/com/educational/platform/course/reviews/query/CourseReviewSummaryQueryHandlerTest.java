package com.educational.platform.course.reviews.query;

import com.educational.platform.course.reviews.CourseReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseReviewSummaryQueryHandlerTest {

	private static final UUID COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

	@Mock
	private CourseReviewRepository repository;

	@InjectMocks
	private CourseReviewSummaryQueryHandler sut;

	@Test
	void handle_ratingsForCourse_summaryComputedFromRepositoryRatings() {
		// given
		when(repository.listRatings(COURSE_UUID)).thenReturn(List.of(4.0, 4.4, 1.0));

		// when
		final CourseReviewSummaryDTO result = sut.handle(new CourseReviewSummaryQuery(COURSE_UUID));

		// then
		verify(repository).listRatings(COURSE_UUID);
		assertThat(result.totalReviews()).isEqualTo(3);
		assertThat(result.averageRating()).isCloseTo(3.1333, within(0.0001));
		assertThat(result.ratingCounts()).containsExactlyInAnyOrderEntriesOf(Map.of(1, 1L, 2, 0L, 3, 0L, 4, 2L, 5, 0L));
	}

	@Test
	void handle_noRatings_zeroSummaryWithEveryStarLevel() {
		// given
		when(repository.listRatings(COURSE_UUID)).thenReturn(List.of());

		// when
		final CourseReviewSummaryDTO result = sut.handle(new CourseReviewSummaryQuery(COURSE_UUID));

		// then
		assertThat(result.totalReviews()).isZero();
		assertThat(result.averageRating()).isZero();
		assertThat(result.ratingCounts()).containsOnlyKeys(1, 2, 3, 4, 5).containsValues(0L).doesNotContainValue(1L);
	}
}
