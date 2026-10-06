package com.educational.platform.course.reviews.query;

import com.educational.platform.course.reviews.CourseReviewRepository;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Query handler for getting the aggregate rating breakdown of a course.
 */
@Component
public class CourseReviewSummaryQueryHandler {

	private final CourseReviewRepository repository;

	public CourseReviewSummaryQueryHandler(CourseReviewRepository repository) {
		this.repository = repository;
	}

	/**
	 * Computes the average rating and the number of reviews per star level for a course.
	 *
	 * @param query query.
	 * @return review summary.
	 */
	@Nonnull
	@Transactional(readOnly = true)
	public CourseReviewSummaryDTO handle(CourseReviewSummaryQuery query) {
		return CourseReviewSummaryDTO.from(repository.listRatings(query.uuid()));
	}
}
