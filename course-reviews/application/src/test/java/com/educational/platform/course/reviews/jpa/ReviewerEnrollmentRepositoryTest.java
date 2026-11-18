package com.educational.platform.course.reviews.jpa;

import com.educational.platform.course.reviews.enrollment.ReviewerEnrollment;
import com.educational.platform.course.reviews.enrollment.ReviewerEnrollmentRepository;
import com.educational.platform.course.reviews.enrollment.create.CreateReviewerEnrollmentCommand;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Sql(scripts = "classpath:course_review.sql")
@DataJpaTest
public class ReviewerEnrollmentRepositoryTest {

	public static final UUID ENROLLED_COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
	public static final UUID NOT_ENROLLED_COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440009");
	public static final String REVIEWER_USERNAME = "reviewer";

	@Autowired
	private ReviewerEnrollmentRepository sut;

	@Test
	void existsByCourseIdAndUsername_enrolledReviewer_true() {
		// given/when
		var result = sut.existsByCourseIdAndUsername(ENROLLED_COURSE_UUID, REVIEWER_USERNAME);

		// then
		assertThat(result).isTrue();
	}

	@Test
	void existsByCourseIdAndUsername_anotherReviewer_false() {
		// given/when
		var result = sut.existsByCourseIdAndUsername(ENROLLED_COURSE_UUID, "another-reviewer");

		// then
		assertThat(result).isFalse();
	}

	@Test
	void existsByCourseIdAndUsername_unknownCourse_false() {
		// given/when
		var result = sut.existsByCourseIdAndUsername(NOT_ENROLLED_COURSE_UUID, REVIEWER_USERNAME);

		// then
		assertThat(result).isFalse();
	}

	@Test
	void save_newEnrollment_persistedAndVisible() {
		// given
		var enrollment = new ReviewerEnrollment(new CreateReviewerEnrollmentCommand(NOT_ENROLLED_COURSE_UUID, REVIEWER_USERNAME));

		// when
		sut.saveAndFlush(enrollment);

		// then
		assertThat(enrollment.getId()).isNotNull();
		assertThat(sut.existsByCourseIdAndUsername(NOT_ENROLLED_COURSE_UUID, REVIEWER_USERNAME)).isTrue();
	}

	@Test
	void save_duplicateCourseAndUsername_dataIntegrityViolationException() {
		// given
		var duplicate = new ReviewerEnrollment(new CreateReviewerEnrollmentCommand(ENROLLED_COURSE_UUID, REVIEWER_USERNAME));

		// when
		final ThrowingCallable saveAction = () -> sut.saveAndFlush(duplicate);

		// then
		assertThatThrownBy(saveAction).isInstanceOf(DataIntegrityViolationException.class);
	}
}
