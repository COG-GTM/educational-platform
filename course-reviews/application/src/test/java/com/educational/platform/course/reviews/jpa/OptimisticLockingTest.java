package com.educational.platform.course.reviews.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.UUID;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;

import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewDTO;
import com.educational.platform.course.reviews.CourseReviewFactory;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.CurrentUserAsReviewer;
import com.educational.platform.course.reviews.course.ReviewableCourse;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommandHandler;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.create.ReviewCourseCommandHandler;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommandHandler;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommandHandler;

/**
 * Verifies the JPA optimistic locking ({@code @Version}) behaviour added to the course-reviews entities:
 * a fresh row starts at version 0, every update increments the version, and a concurrent update of a
 * stale instance fails with an {@link OptimisticLockingFailureException} instead of silently overwriting.
 */
@Sql(scripts = "classpath:course_review.sql")
@DataJpaTest
public class OptimisticLockingTest {

	private static final UUID COURSE_REVIEW_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
	private static final UUID COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

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
	void courseReview_transientBeforePersist_versionIsNull() throws Exception {
		// given - a freshly constructed, not-yet-persisted course review. The domain constructor is
		// package-private to the entity, so it is reached reflectively from this test package.
		final Constructor<CourseReview> constructor = CourseReview.class
				.getDeclaredConstructor(ReviewCourseCommand.class, Integer.class, Integer.class);
		constructor.setAccessible(true);
		final CourseReview review = constructor
				.newInstance(new ReviewCourseCommand(UUID.randomUUID(), 4.0, "transient"), 1, 1);

		// then - the JPA-managed version is null until the row is persisted
		assertThat(ReflectionTestUtils.getField(review, "version")).isNull();

		// when - it is persisted
		final CourseReview saved = courseReviewRepository.saveAndFlush(review);

		// then - Hibernate initialises the version to zero (the null-version condition the seed SQL
		// works around so updating a row does not increment a null version and NPE)
		assertThat(ReflectionTestUtils.getField(saved, "version")).isEqualTo(0);
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

	@Test
	void courseReview_multipleSequentialUpdates_versionIncrementsEachTime() {
		// given
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// when - the row is updated twice in a row (capturing the version after each flush,
		// since saveAndFlush returns the same managed instance whose version keeps mutating)
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "first update"));
		courseReviewRepository.saveAndFlush(review);
		final Object versionAfterFirst = ReflectionTestUtils.getField(review, "version");
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 3.0, "second update"));
		courseReviewRepository.saveAndFlush(review);
		final Object versionAfterSecond = ReflectionTestUtils.getField(review, "version");

		// then - the version is bumped once per update rather than only on the first one
		assertThat(versionAfterFirst).isEqualTo(1);
		assertThat(versionAfterSecond).isEqualTo(2);
	}

	@Test
	void courseReview_multipleSequentialUpdates_finalVersionPersistedToDatabase() {
		// given - the seeded review is updated twice in the same transaction
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "first update"));
		courseReviewRepository.saveAndFlush(review);
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 3.0, "second update"));
		courseReviewRepository.saveAndFlush(review);

		// when - the persistence context is cleared and the row reloaded from the database
		entityManager.clear();
		final CourseReview reloaded = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// then - both increments were actually written: the persisted version is 2, not only the first bump
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
	}

	@Test
	void courseReview_updated_versionPersistedToDatabase() {
		// given - an update is flushed
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "persisted"));
		courseReviewRepository.saveAndFlush(review);

		// when - the persistence context is cleared and the row reloaded from the database
		entityManager.clear();
		final CourseReview reloaded = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// then - the incremented version was actually written to the version column
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
	}

	@Test
	void courseReview_staleDelete_throwsOptimisticLockingFailure() {
		// given - two instances reading the same row at version 0
		final CourseReview stale = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// and - the first update wins, bumping the persisted version to 1
		fresh.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to delete the row
		// then - the version guard rejects the delete instead of removing the newer row
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> {
					courseReviewRepository.delete(stale);
					courseReviewRepository.flush();
				});
	}

	@Test
	void courseReview_currentDelete_succeeds() {
		// given - the seeded review read at its current persisted version
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// when - it is deleted at its current version and flushed
		courseReviewRepository.delete(review);
		courseReviewRepository.flush();

		// then - the version guard permits the delete (the success counterpart to the stale-delete
		// rejection): @Version does not block removing a row read at its current version
		entityManager.clear();
		assertThat(courseReviewRepository.findByUuid(COURSE_REVIEW_UUID)).isEmpty();
	}

	@Test
	void courseReview_updated_newRatingAndCommentPersistedToDatabase() {
		// given - the seeded review is updated with a new rating and comment
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "updated comment"));
		courseReviewRepository.saveAndFlush(review);

		// when - the persistence context is cleared and the row read back through the query API
		entityManager.clear();
		final CourseReviewDTO reloaded = courseReviewRepository.listCourseReviews(COURSE_UUID).get(0);

		// then - the update persisted the new field values, not only the version bump
		assertThat(reloaded.rating()).isEqualTo(5.0);
		assertThat(reloaded.comment()).isEqualTo("updated comment");
	}

	@Test
	void courseReview_concurrentUpdate_winnerChangesPersistedAndStaleRejected() {
		// given - two instances reading the same row at version 0
		final CourseReview stale = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// and - the first writer wins, persisting its rating and comment
		fresh.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// then - the winner's values are the ones actually stored
		entityManager.clear();
		final CourseReviewDTO reloaded = courseReviewRepository.listCourseReviews(COURSE_UUID).get(0);
		assertThat(reloaded.rating()).isEqualTo(5.0);
		assertThat(reloaded.comment()).isEqualTo("first wins");

		// when - the stale instance (still version 0) tries to overwrite with its own values
		stale.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 1.0, "stale loses"));

		// then - it is rejected instead of silently overwriting the winner
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> courseReviewRepository.saveAndFlush(stale));
	}

	@Test
	void courseReview_updatedViaCommandHandler_versionIncrementedAndChangesPersisted() {
		// given - the production update path: the real command handler wired with the real repository
		final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		final UpdateCourseReviewCommandHandler handler = new UpdateCourseReviewCommandHandler(validator, courseReviewRepository);

		// when - the seeded review is updated through the handler (which saves the managed entity)
		handler.handle(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "via handler"));
		entityManager.flush();
		entityManager.clear();

		// then - @Version integrates transparently with the existing update flow: the version bumped
		// and the new field values were persisted, confirming no handler/service change was required
		final CourseReview reloaded = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
		final CourseReviewDTO dto = courseReviewRepository.listCourseReviews(COURSE_UUID).get(0);
		assertThat(dto.rating()).isEqualTo(5.0);
		assertThat(dto.comment()).isEqualTo("via handler");
	}

	@Test
	void courseReview_createdViaCommandHandler_versionInitializedToZero() {
		// given - the production create path: the real factory + command handler wired with the real
		// repositories. Only the security-dependent reviewer lookup is mocked, as in CourseReviewFactoryTest.
		final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		final CurrentUserAsReviewer currentUserAsReviewer = mock(CurrentUserAsReviewer.class);
		when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewerRepository.findByUsername("reviewer"));
		final CourseReviewFactory factory = new CourseReviewFactory(validator, currentUserAsReviewer, reviewableCourseRepository);
		final ReviewCourseCommandHandler handler = new ReviewCourseCommandHandler(courseReviewRepository, factory);

		// when - a brand new review is created for the seeded course through the handler
		final UUID createdUuid = handler.handle(new ReviewCourseCommand(COURSE_UUID, 3.0, "created via handler"));
		entityManager.flush();
		entityManager.clear();

		// then - @Version integrates transparently with the existing create flow: the fresh row starts at
		// version 0 and its field values are persisted, confirming no factory/handler change was required
		final CourseReview created = courseReviewRepository.findByUuid(createdUuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(created, "version")).isEqualTo(0);
		final CourseReviewDTO dto = courseReviewRepository.listCourseReviews(COURSE_UUID).stream()
				.filter(review -> review.uuid().equals(createdUuid))
				.findFirst().orElseThrow();
		assertThat(dto.rating()).isEqualTo(3.0);
		assertThat(dto.comment()).isEqualTo("created via handler");
	}

	@Test
	void courseReview_concurrentUpdateAfterRefresh_succeedsAndVersionIncrements() {
		// given - the first writer wins, bumping the persisted version to 1
		final CourseReview first = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		first.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(first);
		entityManager.clear();

		// when - a second writer re-reads the current state (version 1) before updating, so it is not
		// stale and its update is accepted - the success counterpart to the stale-update rejection
		final CourseReview second = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		second.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 2.0, "second also wins"));
		courseReviewRepository.saveAndFlush(second);

		// then - the version advanced to 2 and the second writer's values are the ones persisted
		entityManager.clear();
		final CourseReview reloaded = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
		final CourseReviewDTO dto = courseReviewRepository.listCourseReviews(COURSE_UUID).get(0);
		assertThat(dto.rating()).isEqualTo(2.0);
		assertThat(dto.comment()).isEqualTo("second also wins");
	}

	@Test
	void courseReview_updatedWithSameValues_versionNotIncremented() {
		// given - the seeded review (version 0, rating 4.0, comment "comment")
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// when - it is "updated" with the values it already has and flushed
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 4.0, "comment"));
		courseReviewRepository.saveAndFlush(review);

		// then - nothing actually changed, so Hibernate issues no UPDATE and the version stays 0
		entityManager.clear();
		final CourseReview reloaded = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(0);
	}

	@Test
	void courseReview_updatingOneReview_otherReviewsVersionUnchanged() throws Exception {
		// given - the seeded review plus a second independently persisted review, both at version 0.
		// The domain constructor is package-private, so it is reached reflectively from this test package.
		final Constructor<CourseReview> constructor = CourseReview.class
				.getDeclaredConstructor(ReviewCourseCommand.class, Integer.class, Integer.class);
		constructor.setAccessible(true);
		final CourseReview second = constructor
				.newInstance(new ReviewCourseCommand(UUID.randomUUID(), 3.0, "second review"), 1, 1);
		final UUID secondUuid = second.toIdentifier();
		courseReviewRepository.saveAndFlush(second);
		entityManager.clear();

		// when - only the second review is updated
		final CourseReview toUpdate = courseReviewRepository.findByUuid(secondUuid).orElseThrow();
		toUpdate.update(new UpdateCourseReviewCommand(secondUuid, 5.0, "second review updated"));
		courseReviewRepository.saveAndFlush(toUpdate);
		entityManager.clear();

		// then - the version bump is isolated to the updated row; the seeded review stays at version 0
		final CourseReview reloadedSeeded = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		final CourseReview reloadedSecond = courseReviewRepository.findByUuid(secondUuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedSecond, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedSeeded, "version")).isEqualTo(0);
	}

	@Test
	void courseReview_staleUpdateAfterConcurrentDelete_throwsOptimisticLockingFailure() {
		// given - two instances reading the same row at version 0
		final CourseReview stale = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// and - the other writer deletes the row out from under the stale instance
		courseReviewRepository.delete(fresh);
		courseReviewRepository.flush();
		entityManager.clear();

		// when - the stale instance (still version 0) tries to update the now-deleted row
		stale.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 1.0, "stale loses"));

		// then - the version guard rejects the update instead of resurrecting the deleted row
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> courseReviewRepository.saveAndFlush(stale));
	}

	@Test
	void courseReview_savedWithoutModification_versionNotIncremented() {
		// given - the seeded review at version 0
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// when - it is re-saved and flushed without changing any field (distinct from re-running
		// update() with identical values: here the entity is not touched at all)
		courseReviewRepository.saveAndFlush(review);

		// then - with nothing dirty Hibernate issues no UPDATE, so the version stays 0
		entityManager.clear();
		final CourseReview reloaded = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(0);
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
	void reviewer_createdViaCommandHandler_versionInitializedToZero() {
		// given - the production create path: the real command handler wired with the real repository
		final CreateReviewerCommandHandler handler = new CreateReviewerCommandHandler(reviewerRepository);

		// when - a reviewer is created through the handler
		handler.handle(new CreateReviewerCommand("created-via-handler"));
		entityManager.flush();
		entityManager.clear();

		// then - the fresh row starts at version 0, confirming @Version needs no create-handler change
		final Reviewer created = reviewerRepository.findByUsername("created-via-handler");
		assertThat(ReflectionTestUtils.getField(created, "version")).isEqualTo(0);
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
	void reviewer_transientBeforePersist_versionIsNull() {
		// given - a freshly constructed, not-yet-persisted reviewer
		final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("transient-reviewer"));

		// then - the JPA-managed version is null until the row is persisted
		assertThat(ReflectionTestUtils.getField(reviewer, "version")).isNull();

		// when - it is persisted
		final Reviewer saved = reviewerRepository.saveAndFlush(reviewer);

		// then - Hibernate initialises the version to zero
		assertThat(ReflectionTestUtils.getField(saved, "version")).isEqualTo(0);
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

	@Test
	void reviewer_concurrentUpdate_winnerChangesPersistedAndStaleRejected() {
		// given - two instances reading the same row at version 0
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();

		final Reviewer stale = reviewerRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final Reviewer fresh = reviewerRepository.findById(id).orElseThrow();

		// and - the first writer wins, persisting its username
		ReflectionTestUtils.setField(fresh, "username", "first-wins");
		reviewerRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// then - the winner's value is the one actually stored
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "username")).isEqualTo("first-wins");
		entityManager.clear();

		// when - the stale instance (still version 0) tries to overwrite with its own value
		ReflectionTestUtils.setField(stale, "username", "stale-loses");

		// then - it is rejected instead of silently overwriting the winner
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewerRepository.saveAndFlush(stale));
	}

	@Test
	void reviewer_multipleSequentialUpdates_versionIncrementsEachTime() {
		// given
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();

		// when - the row is updated twice in a row (capturing the version after each flush,
		// since saveAndFlush returns the same managed instance whose version keeps mutating)
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(reviewer, "username", "renamed-once");
		reviewerRepository.saveAndFlush(reviewer);
		final Object versionAfterFirst = ReflectionTestUtils.getField(reviewer, "version");
		ReflectionTestUtils.setField(reviewer, "username", "renamed-twice");
		reviewerRepository.saveAndFlush(reviewer);
		final Object versionAfterSecond = ReflectionTestUtils.getField(reviewer, "version");

		// then - the version is bumped once per update
		assertThat(versionAfterFirst).isEqualTo(1);
		assertThat(versionAfterSecond).isEqualTo(2);
	}

	@Test
	void reviewer_multipleSequentialUpdates_finalVersionPersistedToDatabase() {
		// given - a persisted reviewer renamed twice in the same transaction
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(reviewer, "username", "renamed-once");
		reviewerRepository.saveAndFlush(reviewer);
		ReflectionTestUtils.setField(reviewer, "username", "renamed-twice");
		reviewerRepository.saveAndFlush(reviewer);

		// when - the persistence context is cleared and the row reloaded from the database
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();

		// then - both increments were actually written: the persisted version is 2, not only the first bump
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
	}

	@Test
	void reviewer_updated_versionPersistedToDatabase() {
		// given - an update is flushed
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(reviewer, "username", "opt-lock-reviewer-renamed");
		reviewerRepository.saveAndFlush(reviewer);

		// when - the persistence context is cleared and the row reloaded from the database
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();

		// then - the incremented version was actually written to the version column
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
	}

	@Test
	void reviewer_updated_newUsernamePersistedToDatabase() {
		// given - a persisted reviewer whose username is changed and flushed
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(reviewer, "username", "opt-lock-reviewer-renamed");
		reviewerRepository.saveAndFlush(reviewer);

		// when - the persistence context is cleared and the row reloaded from the database
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();

		// then - the update wrote the new field value alongside the version bump, not the version alone
		assertThat(ReflectionTestUtils.getField(reloaded, "username")).isEqualTo("opt-lock-reviewer-renamed");
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
	}

	@Test
	void reviewer_staleDelete_throwsOptimisticLockingFailure() {
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

		// when - the stale instance (still version 0) tries to delete the row
		// then - the version guard rejects the delete instead of removing the newer row
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> {
					reviewerRepository.delete(stale);
					reviewerRepository.flush();
				});
	}

	@Test
	void reviewer_currentDelete_succeeds() {
		// given - a persisted reviewer read at its current persisted version
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();

		// when - it is deleted at its current version and flushed
		reviewerRepository.delete(reviewer);
		reviewerRepository.flush();

		// then - the version guard permits the delete (the success counterpart to the stale-delete
		// rejection): @Version does not block removing a row read at its current version
		entityManager.clear();
		assertThat(reviewerRepository.findById(id)).isEmpty();
	}

	@Test
	void reviewer_updatingOneRow_otherRowsVersionUnchanged() {
		// given - two independently persisted reviewers, both starting at version 0
		final Integer firstId = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("reviewer-one"))).getId();
		final Integer secondId = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("reviewer-two"))).getId();
		entityManager.clear();

		// when - only the first reviewer is updated
		final Reviewer first = reviewerRepository.findById(firstId).orElseThrow();
		ReflectionTestUtils.setField(first, "username", "reviewer-one-renamed");
		reviewerRepository.saveAndFlush(first);
		entityManager.clear();

		// then - the version bump is isolated to the updated row; the untouched row stays at version 0
		final Reviewer reloadedFirst = reviewerRepository.findById(firstId).orElseThrow();
		final Reviewer reloadedSecond = reviewerRepository.findById(secondId).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedFirst, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedSecond, "version")).isEqualTo(0);
	}

	@Test
	void reviewer_savedWithoutModification_versionNotIncremented() {
		// given - a persisted reviewer at version 0
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();

		// when - it is re-saved and flushed without changing any field
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		reviewerRepository.saveAndFlush(reviewer);

		// then - with nothing dirty Hibernate issues no UPDATE, so the version stays 0
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(0);
	}

	@Test
	void reviewer_updatedWithSameUsername_versionNotIncremented() {
		// given - a persisted reviewer at version 0
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();

		// when - the username is reassigned to the value it already has and flushed (distinct from
		// savedWithoutModification: here the field is touched, but to an equal value)
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(reviewer, "username", "opt-lock-reviewer");
		reviewerRepository.saveAndFlush(reviewer);

		// then - nothing actually changed, so Hibernate issues no UPDATE and the version stays 0
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(0);
	}

	@Test
	void reviewer_concurrentUpdateAfterRefresh_succeedsAndVersionIncrements() {
		// given - the first writer wins, bumping the persisted version to 1
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();
		final Reviewer first = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(first, "username", "first-wins");
		reviewerRepository.saveAndFlush(first);
		entityManager.clear();

		// when - a second writer re-reads the current state (version 1) before updating, so it is not
		// stale and its update is accepted - the success counterpart to the stale-update rejection
		final Reviewer second = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(second, "username", "second-also-wins");
		reviewerRepository.saveAndFlush(second);

		// then - the version advanced to 2 and the second writer's value is the one persisted
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
		assertThat(ReflectionTestUtils.getField(reloaded, "username")).isEqualTo("second-also-wins");
	}

	@Test
	void reviewer_staleUpdateAfterConcurrentDelete_throwsOptimisticLockingFailure() {
		// given - two instances reading the same row at version 0
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();

		final Reviewer stale = reviewerRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final Reviewer fresh = reviewerRepository.findById(id).orElseThrow();

		// and - the other writer deletes the row out from under the stale instance
		reviewerRepository.delete(fresh);
		reviewerRepository.flush();
		entityManager.clear();

		// when - the stale instance (still version 0) tries to update the now-deleted row
		ReflectionTestUtils.setField(stale, "username", "stale-loses");

		// then - the version guard rejects the update instead of resurrecting the deleted row
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
	void reviewableCourse_createdViaCommandHandler_versionInitializedToZero() {
		// given - the production create path: the real command handler wired with the real repository
		final UUID originalCourseId = UUID.randomUUID();
		final CreateReviewableCourseCommandHandler handler = new CreateReviewableCourseCommandHandler(reviewableCourseRepository);

		// when - a reviewable course is created through the handler
		handler.handle(new CreateReviewableCourseCommand(originalCourseId));
		entityManager.flush();
		entityManager.clear();

		// then - the fresh row starts at version 0, confirming @Version needs no create-handler change
		final ReviewableCourse created = reviewableCourseRepository.findByOriginalCourseId(originalCourseId).orElseThrow();
		assertThat(ReflectionTestUtils.getField(created, "version")).isEqualTo(0);
	}

	@Test
	void reviewableCourse_transientBeforePersist_versionIsNull() {
		// given - a freshly constructed, not-yet-persisted reviewable course
		final ReviewableCourse course = new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()));

		// then - the JPA-managed version is null until the row is persisted
		assertThat(ReflectionTestUtils.getField(course, "version")).isNull();

		// when - it is persisted
		final ReviewableCourse saved = reviewableCourseRepository.saveAndFlush(course);

		// then - Hibernate initialises the version to zero
		assertThat(ReflectionTestUtils.getField(saved, "version")).isEqualTo(0);
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

	@Test
	void reviewableCourse_concurrentUpdate_winnerChangesPersistedAndStaleRejected() {
		// given - two instances reading the same row at version 0
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();

		final ReviewableCourse stale = reviewableCourseRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final ReviewableCourse fresh = reviewableCourseRepository.findById(id).orElseThrow();

		// and - the first writer wins, persisting its originalCourseId
		final UUID winnerCourseId = UUID.randomUUID();
		ReflectionTestUtils.setField(fresh, "originalCourseId", winnerCourseId);
		reviewableCourseRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// then - the winner's value is the one actually stored
		entityManager.clear();
		final ReviewableCourse reloaded = reviewableCourseRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "originalCourseId")).isEqualTo(winnerCourseId);
		entityManager.clear();

		// when - the stale instance (still version 0) tries to overwrite with its own value
		ReflectionTestUtils.setField(stale, "originalCourseId", UUID.randomUUID());

		// then - it is rejected instead of silently overwriting the winner
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewableCourseRepository.saveAndFlush(stale));
	}

	@Test
	void reviewableCourse_multipleSequentialUpdates_versionIncrementsEachTime() {
		// given
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();

		// when - the row is updated twice in a row (capturing the version after each flush,
		// since saveAndFlush returns the same managed instance whose version keeps mutating)
		final ReviewableCourse course = reviewableCourseRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(course, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(course);
		final Object versionAfterFirst = ReflectionTestUtils.getField(course, "version");
		ReflectionTestUtils.setField(course, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(course);
		final Object versionAfterSecond = ReflectionTestUtils.getField(course, "version");

		// then - the version is bumped once per update
		assertThat(versionAfterFirst).isEqualTo(1);
		assertThat(versionAfterSecond).isEqualTo(2);
	}

	@Test
	void reviewableCourse_multipleSequentialUpdates_finalVersionPersistedToDatabase() {
		// given - a persisted reviewable course updated twice in the same transaction
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();
		final ReviewableCourse course = reviewableCourseRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(course, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(course);
		ReflectionTestUtils.setField(course, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(course);

		// when - the persistence context is cleared and the row reloaded from the database
		entityManager.clear();
		final ReviewableCourse reloaded = reviewableCourseRepository.findById(id).orElseThrow();

		// then - both increments were actually written: the persisted version is 2, not only the first bump
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
	}

	@Test
	void reviewableCourse_updated_versionPersistedToDatabase() {
		// given - an update is flushed
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();
		final ReviewableCourse course = reviewableCourseRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(course, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(course);

		// when - the persistence context is cleared and the row reloaded from the database
		entityManager.clear();
		final ReviewableCourse reloaded = reviewableCourseRepository.findById(id).orElseThrow();

		// then - the incremented version was actually written to the version column
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
	}

	@Test
	void reviewableCourse_updated_newOriginalCourseIdPersistedToDatabase() {
		// given - a persisted reviewable course whose originalCourseId is changed and flushed
		final UUID updatedCourseId = UUID.randomUUID();
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();
		final ReviewableCourse course = reviewableCourseRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(course, "originalCourseId", updatedCourseId);
		reviewableCourseRepository.saveAndFlush(course);

		// when - the persistence context is cleared and the row reloaded from the database
		entityManager.clear();
		final ReviewableCourse reloaded = reviewableCourseRepository.findById(id).orElseThrow();

		// then - the update wrote the new field value alongside the version bump, not the version alone
		assertThat(ReflectionTestUtils.getField(reloaded, "originalCourseId")).isEqualTo(updatedCourseId);
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
	}

	@Test
	void reviewableCourse_staleDelete_throwsOptimisticLockingFailure() {
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

		// when - the stale instance (still version 0) tries to delete the row
		// then - the version guard rejects the delete instead of removing the newer row
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> {
					reviewableCourseRepository.delete(stale);
					reviewableCourseRepository.flush();
				});
	}

	@Test
	void reviewableCourse_currentDelete_succeeds() {
		// given - a persisted reviewable course read at its current persisted version
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();
		final ReviewableCourse course = reviewableCourseRepository.findById(id).orElseThrow();

		// when - it is deleted at its current version and flushed
		reviewableCourseRepository.delete(course);
		reviewableCourseRepository.flush();

		// then - the version guard permits the delete (the success counterpart to the stale-delete
		// rejection): @Version does not block removing a row read at its current version
		entityManager.clear();
		assertThat(reviewableCourseRepository.findById(id)).isEmpty();
	}

	@Test
	void reviewableCourse_savedWithoutModification_versionNotIncremented() {
		// given - a persisted reviewable course at version 0
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();

		// when - it is re-saved and flushed without changing any field
		final ReviewableCourse course = reviewableCourseRepository.findById(id).orElseThrow();
		reviewableCourseRepository.saveAndFlush(course);

		// then - with nothing dirty Hibernate issues no UPDATE, so the version stays 0
		entityManager.clear();
		final ReviewableCourse reloaded = reviewableCourseRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(0);
	}

	@Test
	void reviewableCourse_updatedWithSameOriginalCourseId_versionNotIncremented() {
		// given - a persisted reviewable course at version 0
		final UUID originalCourseId = UUID.randomUUID();
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(originalCourseId))).getId();
		entityManager.clear();

		// when - the originalCourseId is reassigned to the value it already has and flushed (distinct from
		// savedWithoutModification: here the field is touched, but to an equal value)
		final ReviewableCourse course = reviewableCourseRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(course, "originalCourseId", originalCourseId);
		reviewableCourseRepository.saveAndFlush(course);

		// then - nothing actually changed, so Hibernate issues no UPDATE and the version stays 0
		entityManager.clear();
		final ReviewableCourse reloaded = reviewableCourseRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(0);
	}

	@Test
	void reviewableCourse_updatingOneRow_otherRowsVersionUnchanged() {
		// given - two independently persisted reviewable courses, both starting at version 0
		final Integer firstId = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		final Integer secondId = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();

		// when - only the first course is updated
		final ReviewableCourse first = reviewableCourseRepository.findById(firstId).orElseThrow();
		ReflectionTestUtils.setField(first, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(first);
		entityManager.clear();

		// then - the version bump is isolated to the updated row; the untouched row stays at version 0
		final ReviewableCourse reloadedFirst = reviewableCourseRepository.findById(firstId).orElseThrow();
		final ReviewableCourse reloadedSecond = reviewableCourseRepository.findById(secondId).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedFirst, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedSecond, "version")).isEqualTo(0);
	}

	@Test
	void reviewableCourse_concurrentUpdateAfterRefresh_succeedsAndVersionIncrements() {
		// given - the first writer wins, bumping the persisted version to 1
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();
		final ReviewableCourse first = reviewableCourseRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(first, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(first);
		entityManager.clear();

		// when - a second writer re-reads the current state (version 1) before updating, so it is not
		// stale and its update is accepted - the success counterpart to the stale-update rejection
		final UUID secondCourseId = UUID.randomUUID();
		final ReviewableCourse second = reviewableCourseRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(second, "originalCourseId", secondCourseId);
		reviewableCourseRepository.saveAndFlush(second);

		// then - the version advanced to 2 and the second writer's value is the one persisted
		entityManager.clear();
		final ReviewableCourse reloaded = reviewableCourseRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
		assertThat(ReflectionTestUtils.getField(reloaded, "originalCourseId")).isEqualTo(secondCourseId);
	}

	@Test
	void reviewableCourse_staleUpdateAfterConcurrentDelete_throwsOptimisticLockingFailure() {
		// given - two instances reading the same row at version 0
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();

		final ReviewableCourse stale = reviewableCourseRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final ReviewableCourse fresh = reviewableCourseRepository.findById(id).orElseThrow();

		// and - the other writer deletes the row out from under the stale instance
		reviewableCourseRepository.delete(fresh);
		reviewableCourseRepository.flush();
		entityManager.clear();

		// when - the stale instance (still version 0) tries to update the now-deleted row
		ReflectionTestUtils.setField(stale, "originalCourseId", UUID.randomUUID());

		// then - the version guard rejects the update instead of resurrecting the deleted row
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewableCourseRepository.saveAndFlush(stale));
	}
}
