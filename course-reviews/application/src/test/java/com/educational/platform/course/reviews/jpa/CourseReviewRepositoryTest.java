package com.educational.platform.course.reviews.jpa;

import static org.assertj.core.api.Assertions.assertThat;

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
	public static final UUID ANOTHER_COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
	public static final UUID ANOTHER_COURSE_REVIEW_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
	public static final UUID UNKNOWN_COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440099");

	@Autowired
	private CourseReviewRepository sut;

	@Test
	void listCourseReviews_unpaged_courseReviews() {
		// given/when
		var result = sut.listCourseReviews(COURSE_UUID);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).uuid()).isEqualTo(COURSE_REVIEW_UUID);
		assertThat(result.get(0).course()).isEqualTo(COURSE_UUID);
	}

	@Test
	void listCourseReviews_anotherCourse_excludesOtherCoursesReviews() {
		// given/when
		var result = sut.listCourseReviews(ANOTHER_COURSE_UUID);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).uuid()).isEqualTo(ANOTHER_COURSE_REVIEW_UUID);
		assertThat(result.get(0).course()).isEqualTo(ANOTHER_COURSE_UUID);
	}

	@Test
	void listCourseReviews_knownCourse_projectsReviewerCommentAndRating() {
		// given/when
		var result = sut.listCourseReviews(COURSE_UUID);

		// then
		assertThat(result).containsExactly(new CourseReviewDTO(COURSE_REVIEW_UUID, COURSE_UUID, REVIEWER_USERNAME, "comment", 4.0));
	}

	@Test
	void listCourseReviews_anotherCourse_projectsItsOwnReviewerCommentAndRating() {
		// given/when
		var result = sut.listCourseReviews(ANOTHER_COURSE_UUID);

		// then
		assertThat(result).containsExactly(new CourseReviewDTO(ANOTHER_COURSE_REVIEW_UUID, ANOTHER_COURSE_UUID, "another-reviewer", "another comment", 5.0));
	}

	@Test
	void listCourseReviews_courseReviewUuidInsteadOfCourseUuid_empty() {
		// given/when
		var result = sut.listCourseReviews(COURSE_REVIEW_UUID);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void listCourseReviews_nullUuid_empty() {
		// given/when
		var result = sut.listCourseReviews(null);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void listCourseReviews_unknownCourse_empty() {
		// given/when
		var result = sut.listCourseReviews(UNKNOWN_COURSE_UUID);

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
