package com.educational.platform.course.reviews.query;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents the aggregate rating breakdown of a course.
 *
 * @param averageRating average rating, 0 when there are no reviews.
 * @param totalReviews  number of reviews.
 * @param ratingCounts  number of reviews per star level (1-5), every level is present.
 */
public record CourseReviewSummaryDTO(double averageRating, long totalReviews, Map<Integer, Long> ratingCounts) {

	public static final int MIN_STAR = 1;
	public static final int MAX_STAR = 5;

	public static CourseReviewSummaryDTO from(List<Double> ratings) {
		final Map<Integer, Long> counts = new TreeMap<>();
		for (int star = MIN_STAR; star <= MAX_STAR; star++) {
			counts.put(star, 0L);
		}
		double sum = 0;
		for (Double rating : ratings) {
			sum += rating;
			final int star = (int) Math.min(MAX_STAR, Math.max(MIN_STAR, Math.round(rating)));
			counts.merge(star, 1L, Long::sum);
		}
		final double average = ratings.isEmpty() ? 0 : sum / ratings.size();
		return new CourseReviewSummaryDTO(average, ratings.size(), counts);
	}
}
