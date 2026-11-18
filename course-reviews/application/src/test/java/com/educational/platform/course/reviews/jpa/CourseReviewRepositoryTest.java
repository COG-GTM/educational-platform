package com.educational.platform.course.reviews.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import com.educational.platform.course.reviews.CourseReviewRepository;

@Sql(scripts = "classpath:course_review.sql")
@DataJpaTest
public class CourseReviewRepositoryTest {

	public static final UUID COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
	public static final UUID COURSE_REVIEW_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
	public static final String REVIEWER_USERNAME = "reviewer";

	@Autowired
	private CourseReviewRepository sut;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void listCourseReviews_unpaged_courseReviews() {
		// given/when
		var result = sut.listCourseReviews(COURSE_UUID);

		// then
		assertThat(result).hasSize(1);
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

	@Test
	void existsByCourseAndReviewer_existingReview_true() {
		// given
		var course = courseIdOf(COURSE_UUID);
		var reviewer = reviewerIdOf(REVIEWER_USERNAME);

		// when
		var result = sut.existsByCourseAndReviewer(course, reviewer);

		// then
		assertThat(result).isTrue();
	}

	@Test
	void existsByCourseAndReviewer_anotherReviewer_false() {
		// given
		var course = courseIdOf(COURSE_UUID);
		var anotherReviewer = reviewerIdOf("another-reviewer");

		// when
		var result = sut.existsByCourseAndReviewer(course, anotherReviewer);

		// then
		assertThat(result).isFalse();
	}

	@Test
	void existsByCourseAndReviewer_anotherCourse_false() {
		// given
		var anotherCourse = courseIdOf(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));
		var reviewer = reviewerIdOf(REVIEWER_USERNAME);

		// when
		var result = sut.existsByCourseAndReviewer(anotherCourse, reviewer);

		// then
		assertThat(result).isFalse();
	}

	@Test
	void insert_secondReviewOfSameCourseBySameReviewer_uniqueConstraintViolated() {
		// given
		var course = courseIdOf(COURSE_UUID);
		var reviewer = reviewerIdOf(REVIEWER_USERNAME);

		// when
		final ThrowingCallable insertAction = () -> jdbcTemplate.update(
				"INSERT INTO course_review (uuid, rating, comment, reviewer, course) VALUES (?, ?, ?, ?, ?)",
				UUID.fromString("123e4567-e89b-12d3-a456-426655440003"), 3, "second comment", reviewer, course);

		// then
		assertThatThrownBy(insertAction).isInstanceOf(DataIntegrityViolationException.class);
	}

	private Integer courseIdOf(UUID originalCourseId) {
		return jdbcTemplate.queryForObject("SELECT id FROM reviewable_course WHERE original_course_id = ?", Integer.class, originalCourseId);
	}

	private Integer reviewerIdOf(String username) {
		return jdbcTemplate.queryForObject("SELECT id FROM reviewer WHERE username = ?", Integer.class, username);
	}
}
