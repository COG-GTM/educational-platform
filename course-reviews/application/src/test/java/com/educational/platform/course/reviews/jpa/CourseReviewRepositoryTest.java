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
	public static final UUID SECOND_COURSE_REVIEW_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440004");
	public static final UUID COURSE_WITHOUT_REVIEWS_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440005");

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
	@Sql(scripts = "classpath:course_review.sql", statements = {
			"INSERT INTO course_review (uuid, rating, comment) VALUES ('123E4567E89B12D3A456426655440004', 2, 'second comment')",
			"UPDATE course_review SET reviewer = (SELECT reviewer.id FROM reviewer WHERE reviewer.username = 'another-reviewer'), "
					+ "course = (SELECT reviewable_course.id FROM reviewable_course WHERE reviewable_course.original_course_id = '123E4567E89B12D3A456426655440000') "
					+ "WHERE uuid = '123E4567E89B12D3A456426655440004'"
	})
	void listCourseReviews_courseWithSeveralReviews_returnsAllReviewsOfThatCourseOnly() {
		// given/when
		var result = sut.listCourseReviews(COURSE_UUID);
		var anotherResult = sut.listCourseReviews(ANOTHER_COURSE_UUID);

		// then
		assertThat(result).containsExactlyInAnyOrder(
				new CourseReviewDTO(COURSE_REVIEW_UUID, COURSE_UUID, REVIEWER_USERNAME, "comment", 4.0),
				new CourseReviewDTO(SECOND_COURSE_REVIEW_UUID, COURSE_UUID, "another-reviewer", "second comment", 2.0));
		assertThat(anotherResult).containsExactly(new CourseReviewDTO(ANOTHER_COURSE_REVIEW_UUID, ANOTHER_COURSE_UUID, "another-reviewer", "another comment", 5.0));
	}

	@Test
	@Sql(scripts = "classpath:course_review.sql", statements = "INSERT INTO reviewable_course (original_course_id) VALUES ('123E4567E89B12D3A456426655440005')")
	void listCourseReviews_knownCourseWithoutReviews_empty() {
		// given/when
		var result = sut.listCourseReviews(COURSE_WITHOUT_REVIEWS_UUID);

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
