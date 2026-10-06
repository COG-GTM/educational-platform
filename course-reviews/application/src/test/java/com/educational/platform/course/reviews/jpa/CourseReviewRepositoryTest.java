package com.educational.platform.course.reviews.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import com.educational.platform.course.reviews.CourseReviewDTO;
import com.educational.platform.course.reviews.CourseReviewRepository;

@Sql(scripts = "classpath:course_review.sql")
@DataJpaTest
public class CourseReviewRepositoryTest {

	public static final UUID COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
	public static final UUID COURSE_REVIEW_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
	public static final String REVIEWER_USERNAME = "reviewer";

	@Autowired
	private CourseReviewRepository sut;

	@Test
	void listCourseReviews_unpaged_courseReviews() {
		// given/when
		var result = sut.listCourseReviews(COURSE_UUID);

		// then
		assertThat(result).hasSize(1);
	}

	@Test
	@Sql(scripts = "classpath:course_reviews_listing.sql")
	void listCourseReviews_severalCourses_onlyRequestedCourseReviewsMostRecentFirst() {
		// given/when
		var result = sut.listCourseReviews(COURSE_UUID);

		// then
		assertThat(result).hasSize(4);
		assertThat(result).extracting(CourseReviewDTO::comment)
				.containsExactly("newest", "same instant, higher id", "same instant, lower id", "oldest");
		assertThat(result).extracting(CourseReviewDTO::course).containsOnly(COURSE_UUID);
		assertThat(result).extracting(CourseReviewDTO::username).containsOnly(REVIEWER_USERNAME);
		assertThat(result).extracting(CourseReviewDTO::createdDate)
				.doesNotContainNull()
				.isSortedAccordingTo(Comparator.<LocalDateTime>reverseOrder());
		assertThat(result.getFirst().createdDate()).isEqualTo(LocalDateTime.of(2026, 3, 1, 10, 0));
	}

	@Test
	@Sql(scripts = "classpath:course_reviews_listing.sql")
	void listCourseReviews_unknownCourse_empty() {
		// given/when
		var result = sut.listCourseReviews(UUID.fromString("123e4567-e89b-12d3-a456-426655440099"));

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@Sql(scripts = "classpath:course_reviews_listing.sql")
	void listRatings_severalCourses_onlyRequestedCourseRatings() {
		// given/when
		var result = sut.listRatings(COURSE_UUID);

		// then
		assertThat(result).containsExactlyInAnyOrder(2.0, 5.0, 4.0, 3.0);
	}

	@Test
	@Sql(scripts = "classpath:course_reviews_listing.sql")
	void listRatings_unknownCourse_empty() {
		// given/when
		var result = sut.listRatings(UUID.fromString("123e4567-e89b-12d3-a456-426655440099"));

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void isReviewer_validReviewer_true() {
		// given/when
		var result = sut.isReviewer(COURSE_REVIEW_UUID, REVIEWER_USERNAME);

		// then
		assertThat(result).isTrue();
	}

	@Test
	void isReviewer_inValidReviewer_false() {
		// given/when
		var result = sut.isReviewer(COURSE_REVIEW_UUID, "another-reviewer");

		// then
		assertThat(result).isFalse();
	}
}
