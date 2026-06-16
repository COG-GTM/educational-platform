package com.educational.platform.course.reviews.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;

import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;

/**
 * Verifies the JPA optimistic locking ({@code @Version}) behaviour added to the course-reviews entities:
 * a fresh row starts at version 0, every update increments the version, and a concurrent update of a
 * stale instance fails with an {@link OptimisticLockingFailureException} instead of silently overwriting.
 */
@Sql(scripts = "classpath:course_review.sql")
@DataJpaTest
public class OptimisticLockingTest {

	private static final UUID COURSE_REVIEW_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private CourseReviewRepository courseReviewRepository;

	@Autowired
	private ReviewableCourseRepository reviewableCourseRepository;

	@Autowired
	private ReviewerRepository reviewerRepository;

	// --- CourseReview -------------------------------------------------------

	@Test
	void courseReview_persisted_versionInitializedToZero() {
		// given/when
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// then
		assertThat(ReflectionTestUtils.getField(review, "version")).isEqualTo(0);
	}

	@Test
	void courseReview_updated_versionIncremented() {
		// given
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// when
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "updated comment"));
		final CourseReview saved = courseReviewRepository.saveAndFlush(review);

		// then
		assertThat(ReflectionTestUtils.getField(saved, "version")).isEqualTo(1);
	}

	@Test
	void courseReview_concurrentUpdate_throwsOptimisticLockingFailure() {
		// given - two instances reading the same row at version 0
		final CourseReview stale = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// and - the first update wins, bumping the persisted version to 1
		fresh.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		stale.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 1.0, "stale loses"));

		// then
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> courseReviewRepository.saveAndFlush(stale));
	}

	// --- Reviewer -----------------------------------------------------------

	@Test
	void reviewer_persisted_versionInitializedToZero() {
		// given/when
		final Reviewer reviewer = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer")));

		// then
		assertThat(ReflectionTestUtils.getField(reviewer, "version")).isEqualTo(0);
	}

	@Test
	void reviewer_updated_versionIncremented() {
		// given
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();

		// when
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(reviewer, "username", "opt-lock-reviewer-renamed");
		final Reviewer saved = reviewerRepository.saveAndFlush(reviewer);

		// then
		assertThat(ReflectionTestUtils.getField(saved, "version")).isEqualTo(1);
	}

	@Test
	void reviewer_concurrentUpdate_throwsOptimisticLockingFailure() {
		// given - two instances reading the same row at version 0
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();

		final Reviewer stale = reviewerRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final Reviewer fresh = reviewerRepository.findById(id).orElseThrow();

		// and - the first update wins, bumping the persisted version to 1
		ReflectionTestUtils.setField(fresh, "username", "first-wins");
		reviewerRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		ReflectionTestUtils.setField(stale, "username", "stale-loses");

		// then
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewerRepository.saveAndFlush(stale));
	}

	// --- ReviewableCourse ---------------------------------------------------

	@Test
	void reviewableCourse_persisted_versionInitializedToZero() {
		// given/when
		final ReviewableCourse course = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID())));

		// then
		assertThat(ReflectionTestUtils.getField(course, "version")).isEqualTo(0);
	}

	@Test
	void reviewableCourse_updated_versionIncremented() {
		// given
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();

		// when
		final ReviewableCourse course = reviewableCourseRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(course, "originalCourseId", UUID.randomUUID());
		final ReviewableCourse saved = reviewableCourseRepository.saveAndFlush(course);

		// then
		assertThat(ReflectionTestUtils.getField(saved, "version")).isEqualTo(1);
	}

	@Test
	void reviewableCourse_concurrentUpdate_throwsOptimisticLockingFailure() {
		// given - two instances reading the same row at version 0
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();

		final ReviewableCourse stale = reviewableCourseRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final ReviewableCourse fresh = reviewableCourseRepository.findById(id).orElseThrow();

		// and - the first update wins, bumping the persisted version to 1
		ReflectionTestUtils.setField(fresh, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		ReflectionTestUtils.setField(stale, "originalCourseId", UUID.randomUUID());

		// then
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewableCourseRepository.saveAndFlush(stale));
	}
}
