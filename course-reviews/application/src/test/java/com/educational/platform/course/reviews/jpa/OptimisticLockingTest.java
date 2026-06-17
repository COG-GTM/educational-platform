package com.educational.platform.course.reviews.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
	void courseReview_concurrentUpdate_failureIdentifiesEntityAndIsRootedInStaleVersion() {
		// given - two instances reading the same row at version 0
		final CourseReview stale = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		final Object reviewId = ReflectionTestUtils.getField(stale, "id");
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// and - the first update wins, bumping the persisted version to 1
		fresh.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		stale.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 1.0, "stale loses"));

		// then - the failure is the version-specific subtype, names the conflicting entity and row, and
		// is rooted in Hibernate's stale-version detection (the "OptimisticLockException" the PR promises),
		// so callers can distinguish a version conflict from other data-access failures
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> courseReviewRepository.saveAndFlush(stale))
				.satisfies(ex -> {
					assertThat(ex.getPersistentClassName()).isEqualTo(CourseReview.class.getName());
					assertThat(ex.getIdentifier()).isEqualTo(reviewId);
				})
				.withRootCauseInstanceOf(StaleObjectStateException.class);
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
	void courseReview_staleDelete_failureIdentifiesEntityAndIsRootedInStaleVersion() {
		// given - two instances reading the same row at version 0
		final CourseReview stale = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		final Object reviewId = ReflectionTestUtils.getField(stale, "id");
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// and - the first update wins, bumping the persisted version to 1
		fresh.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to delete the row
		// then - the delete guard surfaces the same diagnostics as the update guard: the version-specific
		// subtype, the conflicting entity and row, and a StaleObjectStateException root cause. The existing
		// courseReview_staleDelete_throwsOptimisticLockingFailure only asserts the generic supertype, so a
		// stale delete that silently degraded into an undiagnosable failure would slip past it.
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> {
					courseReviewRepository.delete(stale);
					courseReviewRepository.flush();
				})
				.satisfies(ex -> {
					assertThat(ex.getPersistentClassName()).isEqualTo(CourseReview.class.getName());
					assertThat(ex.getIdentifier()).isEqualTo(reviewId);
				})
				.withRootCauseInstanceOf(StaleObjectStateException.class);
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
	void courseReview_winnerCommittedViaUpdateHandler_staleRepositorySaveRejected() {
		// given - a stale instance captured at version 0, detached before the winning write
		final CourseReview stale = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		final Object reviewId = ReflectionTestUtils.getField(stale, "id");
		entityManager.detach(stale);

		// and - the winning update is committed through the production update handler (read-modify-save),
		// bumping the persisted version to 1 and storing its new values. The existing winner-persisted test
		// commits the winner straight through the repository; here the winner goes through the real use case.
		final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		final UpdateCourseReviewCommandHandler handler = new UpdateCourseReviewCommandHandler(validator, courseReviewRepository);
		handler.handle(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "handler wins"));
		entityManager.flush();
		entityManager.clear();

		// then - the handler's values are the ones actually stored
		final CourseReviewDTO reloaded = courseReviewRepository.listCourseReviews(COURSE_UUID).get(0);
		assertThat(reloaded.rating()).isEqualTo(5.0);
		assertThat(reloaded.comment()).isEqualTo("handler wins");

		// when - the stale instance (still version 0) tries to overwrite the handler's committed change
		stale.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 1.0, "stale loses"));

		// then - the loser is rejected with the version-specific failure (entity + row identified, rooted in
		// stale-version detection) instead of silently overwriting the value the production handler committed
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> courseReviewRepository.saveAndFlush(stale))
				.satisfies(ex -> {
					assertThat(ex.getPersistentClassName()).isEqualTo(CourseReview.class.getName());
					assertThat(ex.getIdentifier()).isEqualTo(reviewId);
				})
				.withRootCauseInstanceOf(StaleObjectStateException.class);
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
	void courseReview_createdThenUpdatedViaCommandHandlers_versionProgressesZeroToOne() {
		// given - both production paths wired with the real repositories: the create factory/handler and the
		// update handler. Only the security-dependent reviewer lookup is mocked, as in CourseReviewFactoryTest.
		final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		final CurrentUserAsReviewer currentUserAsReviewer = mock(CurrentUserAsReviewer.class);
		when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewerRepository.findByUsername("reviewer"));
		final CourseReviewFactory factory = new CourseReviewFactory(validator, currentUserAsReviewer, reviewableCourseRepository);
		final ReviewCourseCommandHandler createHandler = new ReviewCourseCommandHandler(courseReviewRepository, factory);
		final UpdateCourseReviewCommandHandler updateHandler = new UpdateCourseReviewCommandHandler(validator, courseReviewRepository);

		// when - a brand new review is created through the handler. Unlike the seeded review (whose version 0
		// comes from the SQL seed), here @Version owns the value end to end, so the fresh row must start at 0.
		final UUID createdUuid = createHandler.handle(new ReviewCourseCommand(COURSE_UUID, 3.0, "created via handler"));
		entityManager.flush();
		entityManager.clear();
		final CourseReview afterCreate = courseReviewRepository.findByUuid(createdUuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(afterCreate, "version")).isEqualTo(0);

		// and - the same review is then updated through the production update handler
		updateHandler.handle(new UpdateCourseReviewCommand(createdUuid, 5.0, "updated via handler"));
		entityManager.flush();
		entityManager.clear();

		// then - the version progresses 0 -> 1 across the two handler paths with no manual version handling,
		// and the update's field values are the ones persisted
		final CourseReview afterUpdate = courseReviewRepository.findByUuid(createdUuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(afterUpdate, "version")).isEqualTo(1);
		final CourseReviewDTO dto = courseReviewRepository.listCourseReviews(COURSE_UUID).stream()
				.filter(review -> review.uuid().equals(createdUuid))
				.findFirst().orElseThrow();
		assertThat(dto.rating()).isEqualTo(5.0);
		assertThat(dto.comment()).isEqualTo("updated via handler");
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
	void courseReview_concurrentUpdatesToDifferentRows_bothSucceedAndEachVersionIncrements() throws Exception {
		// given - the seeded review plus a second independently persisted review, both at version 0. The domain
		// constructor is package-private, so it is reached reflectively from this test package.
		final Constructor<CourseReview> constructor = CourseReview.class
				.getDeclaredConstructor(ReviewCourseCommand.class, Integer.class, Integer.class);
		constructor.setAccessible(true);
		final CourseReview secondRow = constructor
				.newInstance(new ReviewCourseCommand(UUID.randomUUID(), 3.0, "second review"), 1, 1);
		final UUID secondUuid = secondRow.toIdentifier();
		courseReviewRepository.saveAndFlush(secondRow);
		entityManager.clear();

		// and - both rows are read into independent snapshots before either is written, modelling two concurrent
		// transactions that each loaded a *different* row while both rows were still at version 0
		final CourseReview firstSnapshot = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		entityManager.detach(firstSnapshot);
		final CourseReview secondSnapshot = courseReviewRepository.findByUuid(secondUuid).orElseThrow();
		entityManager.detach(secondSnapshot);

		// when - each snapshot writes its own row, the second one only after the first has already been committed
		firstSnapshot.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "first row updated"));
		courseReviewRepository.saveAndFlush(firstSnapshot);
		secondSnapshot.update(new UpdateCourseReviewCommand(secondUuid, 2.0, "second row updated"));
		courseReviewRepository.saveAndFlush(secondSnapshot);

		// then - the optimistic-lock check is scoped per row, not per table: committing one row does not stale a
		// snapshot of a *different* row, so both writes succeed (neither throws) and each version advances
		// independently to 1. courseReview_updatingOneReview_otherReviewsVersionUnchanged only updates one row and
		// asserts the other is untouched; it never writes a pre-loaded snapshot of the second row after the first
		// was committed, so a guard that wrongly keyed off table-wide state would have slipped past it.
		entityManager.clear();
		final CourseReview reloadedFirst = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		final CourseReview reloadedSecond = courseReviewRepository.findByUuid(secondUuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedFirst, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedSecond, "version")).isEqualTo(1);
	}

	@Test
	void courseReview_updated_linkedReviewerAndReviewableCourseVersionsUnchanged() {
		// given - the seeded review at version 0 together with the reviewer and reviewable_course rows it
		// references by FK id (not by a JPA association), all seeded at version 0
		final Integer reviewerId = reviewerRepository.findByUsername("reviewer").getId();
		final Integer reviewableCourseId = reviewableCourseRepository.findByOriginalCourseId(COURSE_UUID).orElseThrow().getId();
		entityManager.clear();

		// when - only the course review is updated
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "updated comment"));
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// then - the version bump is isolated to the course_review aggregate. The reviewer and reviewable_course
		// are separate aggregates linked only by FK id (there is no JPA cascade), so updating the review must not
		// touch their rows or bump their independent versions. The existing isolation test only covers sibling
		// course_review rows, leaving cross-aggregate independence between the three new @Version columns unproven.
		final CourseReview reloadedReview = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		final Reviewer reloadedReviewer = reviewerRepository.findById(reviewerId).orElseThrow();
		final ReviewableCourse reloadedCourse = reviewableCourseRepository.findById(reviewableCourseId).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedReview, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedReviewer, "version")).isEqualTo(0);
		assertThat(ReflectionTestUtils.getField(reloadedCourse, "version")).isEqualTo(0);
	}

	@Test
	void courseReview_createdReferencingExistingParents_parentVersionsUnchanged() throws Exception {
		// given - the seeded reviewer and reviewable_course (both at version 0), referenced by FK id and not by a
		// JPA association. The domain constructor is package-private, so it is reached reflectively from this package.
		final Integer reviewerId = reviewerRepository.findByUsername("reviewer").getId();
		final Integer reviewableCourseId = reviewableCourseRepository.findByOriginalCourseId(COURSE_UUID).orElseThrow().getId();
		entityManager.clear();
		final Constructor<CourseReview> constructor = CourseReview.class
				.getDeclaredConstructor(ReviewCourseCommand.class, Integer.class, Integer.class);
		constructor.setAccessible(true);
		final CourseReview created = constructor
				.newInstance(new ReviewCourseCommand(UUID.randomUUID(), 3.0, "new review"), reviewableCourseId, reviewerId);
		final UUID createdUuid = created.toIdentifier();

		// when - a brand new review referencing those parents is inserted and flushed
		courseReviewRepository.saveAndFlush(created);
		entityManager.clear();

		// then - inserting the child starts it at version 0 and leaves the referenced parent aggregates untouched:
		// there is no JPA cascade, so the reviewer and reviewable_course keep their independent version 0. The existing
		// cross-aggregate isolation test only covers the update path (courseReview_updated_linkedReviewerAndReviewable
		// CourseVersionsUnchanged); the insert path that first establishes the FK link was unverified, so a stray
		// cascade bumping a parent version on persist would have slipped past it.
		final CourseReview reloadedCreated = courseReviewRepository.findByUuid(createdUuid).orElseThrow();
		final Reviewer reloadedReviewer = reviewerRepository.findById(reviewerId).orElseThrow();
		final ReviewableCourse reloadedCourse = reviewableCourseRepository.findById(reviewableCourseId).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedCreated, "version")).isEqualTo(0);
		assertThat(ReflectionTestUtils.getField(reloadedReviewer, "version")).isEqualTo(0);
		assertThat(ReflectionTestUtils.getField(reloadedCourse, "version")).isEqualTo(0);
	}

	@Test
	void courseReview_partialUpdateChangingOnlyComment_versionIncrementedAndCommentPersisted() {
		// given - the seeded review (version 0, rating 4.0, comment "comment")
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// when - it is updated keeping the same rating (4.0) but a new comment, so only one mapped field actually changes
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 4.0, "only the comment changed"));
		courseReviewRepository.saveAndFlush(review);

		// then - a single dirty field is enough to bump the version. This sits between courseReview_updated_version
		// Incremented (rating and comment both change -> bump) and courseReview_updatedWithSameValues_versionNot
		// Incremented (nothing changes -> no bump), pinning that the version tracks any field change rather than only
		// a change to every field, and that the changed comment is the value persisted
		entityManager.clear();
		final CourseReview reloaded = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
		final CourseReviewDTO dto = courseReviewRepository.listCourseReviews(COURSE_UUID).get(0);
		assertThat(dto.rating()).isEqualTo(4.0);
		assertThat(dto.comment()).isEqualTo("only the comment changed");
	}

	@Test
	void courseReview_partialUpdateChangingOnlyRating_versionIncrementedAndRatingPersisted() {
		// given - the seeded review (version 0, rating 4.0, comment "comment")
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// when - it is updated keeping the same comment ("comment") but a new rating, so only the other mapped field
		// actually changes. courseReview_partialUpdateChangingOnlyComment_versionIncrementedAndCommentPersisted covers
		// the comment-only direction; the rating-only direction was the one single-dirty-field case left unverified.
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "comment"));
		courseReviewRepository.saveAndFlush(review);

		// then - changing only the rating is enough to bump the version, the new rating is persisted, and the
		// untouched comment survives the edit
		entityManager.clear();
		final CourseReview reloaded = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
		final CourseReviewDTO dto = courseReviewRepository.listCourseReviews(COURSE_UUID).get(0);
		assertThat(dto.rating()).isEqualTo(5.0);
		assertThat(dto.comment()).isEqualTo("comment");
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
	void courseReview_concurrentUpdateAfterWinnerAdvancedMultipleVersions_throwsOptimisticLockingFailure() {
		// given - a stale instance captured at version 0
		final CourseReview stale = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		entityManager.detach(stale);

		// and - the winning writer advances the row by more than one version (0 -> 1 -> 2) before the stale
		// instance saves, so the gap between the stale version and the persisted version is greater than one
		final CourseReview fresh = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		fresh.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(fresh);
		fresh.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 4.5, "first wins again"));
		courseReviewRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		stale.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 1.0, "stale loses"));

		// then - the version guard rejects the stale write regardless of how far the row has advanced, not
		// only when it is exactly one version behind (the existing concurrentUpdate tests cover a gap of one)
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> courseReviewRepository.saveAndFlush(stale));
	}

	@Test
	void courseReview_deleteAfterConcurrentDelete_isNoOpAndRowRemainsDeleted() {
		// given - two instances reading the same row at version 0
		final CourseReview stale = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();

		// and - the other writer deletes the row first
		courseReviewRepository.delete(fresh);
		courseReviewRepository.flush();
		entityManager.clear();

		// when - the stale instance tries to delete the row that is already gone
		// then - the delete is idempotent: the version guard raises no spurious optimistic-lock failure for a
		// row that simply no longer exists (the delete-path counterpart to staleUpdateAfterConcurrentDelete,
		// where the resurrecting update is rejected), and the row stays deleted
		courseReviewRepository.delete(stale);
		courseReviewRepository.flush();
		entityManager.clear();
		assertThat(courseReviewRepository.findByUuid(COURSE_REVIEW_UUID)).isEmpty();
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

	@Test
	void courseReview_listCourseReviews_seededReviewProjectsAllFieldsWithNoVersionLeak() {
		// when - the seeded review is read through the public query projection, which joins
		// course_review -> reviewable_course -> reviewer (all three gained a version column in this PR)
		final List<CourseReviewDTO> reviews = courseReviewRepository.listCourseReviews(COURSE_UUID);

		// then - the projection is intact: every DTO field still resolves across the joined tables, and the
		// new internal @Version field is not part of the public read contract (CourseReviewDTO exposes no version),
		// confirming the PR's claim that no repository/read-side code had to change
		assertThat(reviews).hasSize(1);
		final CourseReviewDTO dto = reviews.get(0);
		assertThat(dto.uuid()).isEqualTo(COURSE_REVIEW_UUID);
		assertThat(dto.course()).isEqualTo(COURSE_UUID);
		assertThat(dto.username()).isEqualTo("reviewer");
		assertThat(dto.comment()).isEqualTo("comment");
		assertThat(dto.rating()).isEqualTo(4.0);
	}

	@Test
	void courseReview_updatedBumpingVersion_isReviewerQueryStillResolves() {
		// given - the seeded review is updated, which bumps its version to 1
		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "updated comment"));
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// then - the existing reviewer-identity query is unaffected by the version mechanics: it still
		// resolves the original reviewer and rejects a non-reviewer after the version-bumping update
		assertThat(courseReviewRepository.isReviewer(COURSE_REVIEW_UUID, "reviewer")).isTrue();
		assertThat(courseReviewRepository.isReviewer(COURSE_REVIEW_UUID, "another-reviewer")).isFalse();
	}

	@Test
	void courseReview_allJoinedTablesVersionBumped_listCourseReviewsStillProjectsEveryField() {
		// given - every table the listCourseReviews projection joins is mutated, so all three rows this PR
		// versioned advance to version 1: the reviewable_course's originalCourseId, the reviewer's username
		// and the course_review's own rating/comment. None of the foreign keys change, so the joins still match.
		final UUID newOriginalCourseId = UUID.randomUUID();
		final ReviewableCourse course = reviewableCourseRepository.findByOriginalCourseId(COURSE_UUID).orElseThrow();
		ReflectionTestUtils.setField(course, "originalCourseId", newOriginalCourseId);
		reviewableCourseRepository.saveAndFlush(course);

		final Reviewer reviewer = reviewerRepository.findByUsername("reviewer");
		ReflectionTestUtils.setField(reviewer, "username", "reviewer-renamed");
		reviewerRepository.saveAndFlush(reviewer);

		final CourseReview review = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		review.update(new UpdateCourseReviewCommand(COURSE_REVIEW_UUID, 5.0, "updated comment"));
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// then - each of the three joined rows really sits at version 1 now
		assertThat(ReflectionTestUtils.getField(
				reviewableCourseRepository.findByOriginalCourseId(newOriginalCourseId).orElseThrow(), "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reviewerRepository.findByUsername("reviewer-renamed"), "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(
				courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow(), "version")).isEqualTo(1);

		// and - the three-way join/projection stays fully intact when every joined table sits at a non-zero
		// version: every DTO field still resolves to its now-current value. listCourseReviews_seededReviewProjects...
		// pins the projection while all rows are at the initial version 0; this pins it once every joined table
		// this PR versioned has advanced past it, so the added version columns cannot quietly corrupt the read join.
		final List<CourseReviewDTO> reviews = courseReviewRepository.listCourseReviews(newOriginalCourseId);
		assertThat(reviews).hasSize(1);
		final CourseReviewDTO dto = reviews.get(0);
		assertThat(dto.uuid()).isEqualTo(COURSE_REVIEW_UUID);
		assertThat(dto.course()).isEqualTo(newOriginalCourseId);
		assertThat(dto.username()).isEqualTo("reviewer-renamed");
		assertThat(dto.comment()).isEqualTo("updated comment");
		assertThat(dto.rating()).isEqualTo(5.0);
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
	void reviewer_concurrentUpdate_failureIdentifiesEntityAndIsRootedInStaleVersion() {
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

		// then - the failure is the version-specific subtype, names the conflicting entity and row, and
		// is rooted in Hibernate's stale-version detection (the "OptimisticLockException" the PR promises),
		// so callers can distinguish a version conflict from other data-access failures
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewerRepository.saveAndFlush(stale))
				.satisfies(ex -> {
					assertThat(ex.getPersistentClassName()).isEqualTo(Reviewer.class.getName());
					assertThat(ex.getIdentifier()).isEqualTo(id);
				})
				.withRootCauseInstanceOf(StaleObjectStateException.class);
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
	void reviewer_staleDelete_failureIdentifiesEntityAndIsRootedInStaleVersion() {
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
		// then - the delete guard surfaces the same diagnostic contract the update guard does: the
		// version-specific subtype, the conflicting entity and row, and a StaleObjectStateException root cause.
		// reviewer_staleDelete_throwsOptimisticLockingFailure only asserts the generic supertype, so a stale
		// delete that degraded into an undiagnosable data-access failure would slip past it (this pins the
		// delete-guard diagnostics for the reviewer the way courseReview_staleDelete_failureIdentifies... does
		// for the headline aggregate).
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> {
					reviewerRepository.delete(stale);
					reviewerRepository.flush();
				})
				.satisfies(ex -> {
					assertThat(ex.getPersistentClassName()).isEqualTo(Reviewer.class.getName());
					assertThat(ex.getIdentifier()).isEqualTo(id);
				})
				.withRootCauseInstanceOf(StaleObjectStateException.class);
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
	void reviewer_concurrentUpdatesToDifferentRows_bothSucceedAndEachVersionIncrements() {
		// given - two independently persisted reviewers, both starting at version 0
		final Integer firstId = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("row-one"))).getId();
		final Integer secondId = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("row-two"))).getId();
		entityManager.clear();

		// and - both rows are read into independent snapshots before either is written, modelling two concurrent
		// transactions that each loaded a *different* row while both rows were still at version 0
		final Reviewer firstSnapshot = reviewerRepository.findById(firstId).orElseThrow();
		entityManager.detach(firstSnapshot);
		final Reviewer secondSnapshot = reviewerRepository.findById(secondId).orElseThrow();
		entityManager.detach(secondSnapshot);

		// when - each snapshot writes its own row, the second one only after the first has already been committed
		ReflectionTestUtils.setField(firstSnapshot, "username", "row-one-renamed");
		reviewerRepository.saveAndFlush(firstSnapshot);
		ReflectionTestUtils.setField(secondSnapshot, "username", "row-two-renamed");
		reviewerRepository.saveAndFlush(secondSnapshot);

		// then - the optimistic-lock check is scoped per row, not per table: committing one row does not stale a
		// snapshot of a *different* row, so both writes succeed (neither throws) and each version advances
		// independently to 1. reviewer_updatingOneRow_otherRowsVersionUnchanged only updates one row and asserts the
		// other is untouched; it never writes a pre-loaded snapshot of the second row after the first was committed,
		// so a guard that wrongly keyed off table-wide state would have slipped past it.
		entityManager.clear();
		final Reviewer reloadedFirst = reviewerRepository.findById(firstId).orElseThrow();
		final Reviewer reloadedSecond = reviewerRepository.findById(secondId).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedFirst, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedSecond, "version")).isEqualTo(1);
	}

	@Test
	void reviewer_updated_linkedCourseReviewVersionUnchanged() {
		// given - the seeded reviewer at version 0 and the course_review that references it by FK id
		final Integer reviewerId = reviewerRepository.findByUsername("reviewer").getId();
		entityManager.clear();

		// when - only the reviewer is updated
		final Reviewer reviewer = reviewerRepository.findById(reviewerId).orElseThrow();
		ReflectionTestUtils.setField(reviewer, "username", "reviewer-renamed");
		reviewerRepository.saveAndFlush(reviewer);
		entityManager.clear();

		// then - the reviewer's version bumps but the review that references it (a separate aggregate) stays at
		// version 0, confirming the parent update does not cascade a version bump onto the referencing course_review
		final Reviewer reloadedReviewer = reviewerRepository.findById(reviewerId).orElseThrow();
		final CourseReview reloadedReview = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedReviewer, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedReview, "version")).isEqualTo(0);
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

	@Test
	void reviewer_concurrentUpdateAfterWinnerAdvancedMultipleVersions_throwsOptimisticLockingFailure() {
		// given - a stale instance captured at version 0
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();

		final Reviewer stale = reviewerRepository.findById(id).orElseThrow();
		entityManager.detach(stale);

		// and - the winning writer advances the row by more than one version (0 -> 1 -> 2)
		final Reviewer fresh = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(fresh, "username", "first-wins");
		reviewerRepository.saveAndFlush(fresh);
		ReflectionTestUtils.setField(fresh, "username", "first-wins-again");
		reviewerRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		ReflectionTestUtils.setField(stale, "username", "stale-loses");

		// then - the version guard rejects the stale write regardless of how far the row has advanced, not
		// only when it is exactly one version behind (the existing concurrentUpdate tests cover a gap of one)
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewerRepository.saveAndFlush(stale));
	}

	@Test
	void reviewer_deleteAfterConcurrentDelete_isNoOpAndRowRemainsDeleted() {
		// given - two instances reading the same row at version 0
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("opt-lock-reviewer"))).getId();
		entityManager.clear();

		final Reviewer stale = reviewerRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final Reviewer fresh = reviewerRepository.findById(id).orElseThrow();

		// and - the other writer deletes the row first
		reviewerRepository.delete(fresh);
		reviewerRepository.flush();
		entityManager.clear();

		// when - the stale instance tries to delete the row that is already gone
		// then - the delete is idempotent: no spurious optimistic-lock failure for a row that no longer exists
		// (the delete-path counterpart to reviewer_staleUpdateAfterConcurrentDelete), and the row stays deleted
		reviewerRepository.delete(stale);
		reviewerRepository.flush();
		entityManager.clear();
		assertThat(reviewerRepository.findById(id)).isEmpty();
	}

	@Test
	void reviewer_versionBumped_listCourseReviewsAndIsReviewerStillResolveAcrossJoin() {
		// given - the seeded reviewer is renamed, bumping the reviewer row this PR versioned to version 1.
		// The seeded course_review joins to this row (by id) for the username the read queries project/match on.
		final Reviewer seeded = reviewerRepository.findByUsername("reviewer");
		ReflectionTestUtils.setField(seeded, "username", "reviewer-renamed");
		reviewerRepository.saveAndFlush(seeded);
		entityManager.clear();

		// then - the joined reviewer row really advanced to version 1
		assertThat(ReflectionTestUtils.getField(reviewerRepository.findByUsername("reviewer-renamed"), "version")).isEqualTo(1);

		// and - the read queries that join course_review -> reviewer are unaffected by the parent's version bump:
		// listCourseReviews still projects the now-current username across the join, and isReviewer resolves the
		// renamed reviewer while rejecting the stale name. courseReview_updatedBumpingVersion_isReviewerQueryStillResolves
		// only bumps the course_review version; the joined reviewer table this PR also versioned was never checked.
		final List<CourseReviewDTO> reviews = courseReviewRepository.listCourseReviews(COURSE_UUID);
		assertThat(reviews).hasSize(1);
		assertThat(reviews.get(0).uuid()).isEqualTo(COURSE_REVIEW_UUID);
		assertThat(reviews.get(0).username()).isEqualTo("reviewer-renamed");
		assertThat(courseReviewRepository.isReviewer(COURSE_REVIEW_UUID, "reviewer-renamed")).isTrue();
		assertThat(courseReviewRepository.isReviewer(COURSE_REVIEW_UUID, "reviewer")).isFalse();
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
	void reviewableCourse_concurrentUpdate_failureIdentifiesEntityAndIsRootedInStaleVersion() {
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

		// then - the failure is the version-specific subtype, names the conflicting entity and row, and
		// is rooted in Hibernate's stale-version detection (the "OptimisticLockException" the PR promises),
		// so callers can distinguish a version conflict from other data-access failures
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewableCourseRepository.saveAndFlush(stale))
				.satisfies(ex -> {
					assertThat(ex.getPersistentClassName()).isEqualTo(ReviewableCourse.class.getName());
					assertThat(ex.getIdentifier()).isEqualTo(id);
				})
				.withRootCauseInstanceOf(StaleObjectStateException.class);
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
	void reviewableCourse_staleDelete_failureIdentifiesEntityAndIsRootedInStaleVersion() {
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
		// then - the delete guard surfaces the full diagnostic contract (version-specific subtype, conflicting
		// entity and row, StaleObjectStateException root cause), matching the update-guard diagnostic test and
		// the course_review stale-delete diagnostic. reviewableCourse_staleDelete_throwsOptimisticLockingFailure
		// only asserts the generic supertype, leaving the delete guard's diagnostics unverified for this aggregate.
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> {
					reviewableCourseRepository.delete(stale);
					reviewableCourseRepository.flush();
				})
				.satisfies(ex -> {
					assertThat(ex.getPersistentClassName()).isEqualTo(ReviewableCourse.class.getName());
					assertThat(ex.getIdentifier()).isEqualTo(id);
				})
				.withRootCauseInstanceOf(StaleObjectStateException.class);
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
	void reviewableCourse_concurrentUpdatesToDifferentRows_bothSucceedAndEachVersionIncrements() {
		// given - two independently persisted reviewable courses, both starting at version 0
		final Integer firstId = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		final Integer secondId = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();

		// and - both rows are read into independent snapshots before either is written, modelling two concurrent
		// transactions that each loaded a *different* row while both rows were still at version 0
		final ReviewableCourse firstSnapshot = reviewableCourseRepository.findById(firstId).orElseThrow();
		entityManager.detach(firstSnapshot);
		final ReviewableCourse secondSnapshot = reviewableCourseRepository.findById(secondId).orElseThrow();
		entityManager.detach(secondSnapshot);

		// when - each snapshot writes its own row, the second one only after the first has already been committed
		ReflectionTestUtils.setField(firstSnapshot, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(firstSnapshot);
		ReflectionTestUtils.setField(secondSnapshot, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(secondSnapshot);

		// then - the optimistic-lock check is scoped per row, not per table: committing one row does not stale a
		// snapshot of a *different* row, so both writes succeed (neither throws) and each version advances
		// independently to 1. reviewableCourse_updatingOneRow_otherRowsVersionUnchanged only updates one row and
		// asserts the other is untouched; it never writes a pre-loaded snapshot of the second row after the first was
		// committed, so a guard that wrongly keyed off table-wide state would have slipped past it.
		entityManager.clear();
		final ReviewableCourse reloadedFirst = reviewableCourseRepository.findById(firstId).orElseThrow();
		final ReviewableCourse reloadedSecond = reviewableCourseRepository.findById(secondId).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedFirst, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedSecond, "version")).isEqualTo(1);
	}

	@Test
	void reviewableCourse_updated_linkedCourseReviewVersionUnchanged() {
		// given - the seeded reviewable_course at version 0 and the course_review that references it by FK id
		final Integer reviewableCourseId = reviewableCourseRepository.findByOriginalCourseId(COURSE_UUID).orElseThrow().getId();
		entityManager.clear();

		// when - only the reviewable_course is updated
		final ReviewableCourse course = reviewableCourseRepository.findById(reviewableCourseId).orElseThrow();
		ReflectionTestUtils.setField(course, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(course);
		entityManager.clear();

		// then - the reviewable_course version bumps but the review that references it (a separate aggregate)
		// stays at version 0, confirming the parent update does not cascade a version bump onto the course_review
		final ReviewableCourse reloadedCourse = reviewableCourseRepository.findById(reviewableCourseId).orElseThrow();
		final CourseReview reloadedReview = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedCourse, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedReview, "version")).isEqualTo(0);
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

	@Test
	void reviewableCourse_concurrentUpdateAfterWinnerAdvancedMultipleVersions_throwsOptimisticLockingFailure() {
		// given - a stale instance captured at version 0
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();

		final ReviewableCourse stale = reviewableCourseRepository.findById(id).orElseThrow();
		entityManager.detach(stale);

		// and - the winning writer advances the row by more than one version (0 -> 1 -> 2)
		final ReviewableCourse fresh = reviewableCourseRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(fresh, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(fresh);
		ReflectionTestUtils.setField(fresh, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		ReflectionTestUtils.setField(stale, "originalCourseId", UUID.randomUUID());

		// then - the version guard rejects the stale write regardless of how far the row has advanced, not
		// only when it is exactly one version behind (the existing concurrentUpdate tests cover a gap of one)
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewableCourseRepository.saveAndFlush(stale));
	}

	@Test
	void reviewableCourse_deleteAfterConcurrentDelete_isNoOpAndRowRemainsDeleted() {
		// given - two instances reading the same row at version 0
		final Integer id = reviewableCourseRepository
				.saveAndFlush(new ReviewableCourse(new CreateReviewableCourseCommand(UUID.randomUUID()))).getId();
		entityManager.clear();

		final ReviewableCourse stale = reviewableCourseRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final ReviewableCourse fresh = reviewableCourseRepository.findById(id).orElseThrow();

		// and - the other writer deletes the row first
		reviewableCourseRepository.delete(fresh);
		reviewableCourseRepository.flush();
		entityManager.clear();

		// when - the stale instance tries to delete the row that is already gone
		// then - the delete is idempotent: no spurious optimistic-lock failure for a row that no longer exists
		// (the delete-path counterpart to reviewableCourse_staleUpdateAfterConcurrentDelete), and the row stays deleted
		reviewableCourseRepository.delete(stale);
		reviewableCourseRepository.flush();
		entityManager.clear();
		assertThat(reviewableCourseRepository.findById(id)).isEmpty();
	}

	@Test
	void reviewableCourse_versionBumped_listCourseReviewsStillProjectsCourseAcrossJoin() {
		// given - the seeded reviewable_course's originalCourseId is changed, bumping the row this PR versioned
		// to version 1. The seeded course_review joins to this row by id (unchanged), and the query projects the
		// course's originalCourseId as the review's course field.
		final UUID newOriginalCourseId = UUID.randomUUID();
		final ReviewableCourse seeded = reviewableCourseRepository.findByOriginalCourseId(COURSE_UUID).orElseThrow();
		ReflectionTestUtils.setField(seeded, "originalCourseId", newOriginalCourseId);
		reviewableCourseRepository.saveAndFlush(seeded);
		entityManager.clear();

		// then - the joined reviewable_course row really advanced to version 1
		assertThat(ReflectionTestUtils.getField(
				reviewableCourseRepository.findByOriginalCourseId(newOriginalCourseId).orElseThrow(), "version")).isEqualTo(1);

		// and - listCourseReviews still resolves the join to the version-bumped parent and projects its now-current
		// originalCourseId as the review's course. Only the course_review version bump was previously checked for
		// read resilience; the joined reviewable_course table this PR also versioned was not.
		final List<CourseReviewDTO> reviews = courseReviewRepository.listCourseReviews(newOriginalCourseId);
		assertThat(reviews).hasSize(1);
		assertThat(reviews.get(0).uuid()).isEqualTo(COURSE_REVIEW_UUID);
		assertThat(reviews.get(0).course()).isEqualTo(newOriginalCourseId);
	}

	// --- Seed fixture (course_review.sql) -----------------------------------

	@Test
	void seededFixtureRows_versionInitializedToZero() {
		// when - the three rows the course_review.sql fixture inserts are read back through the public
		// repositories. The fixture was changed in this PR to seed version = 0 on all three tables.
		final CourseReview seededReview = courseReviewRepository.findByUuid(COURSE_REVIEW_UUID).orElseThrow();
		final Reviewer seededReviewer = reviewerRepository.findByUsername("reviewer");
		final ReviewableCourse seededCourse = reviewableCourseRepository.findByOriginalCourseId(COURSE_UUID).orElseThrow();

		// then - every seeded row starts at version 0. Other tests assert this for the seeded course_review,
		// but the seeded reviewer and reviewable_course versions (added by this PR's fixture edit) were never
		// asserted; a missing version on either seed column would resurface the null-version NPE the edit fixes.
		assertThat(ReflectionTestUtils.getField(seededReview, "version")).isEqualTo(0);
		assertThat(ReflectionTestUtils.getField(seededReviewer, "version")).isEqualTo(0);
		assertThat(ReflectionTestUtils.getField(seededCourse, "version")).isEqualTo(0);
	}

	@Test
	void anotherSeededReviewer_versionInitializedToZero() {
		// given/when - the second reviewer the course_review.sql fixture inserts ("another-reviewer"),
		// whose version this PR's fixture edit also seeds to 0
		final Reviewer seeded = reviewerRepository.findByUsername("another-reviewer");

		// then - it starts at version 0 like the other seeded rows. seededFixtureRows_versionInitializedToZero
		// asserts this for the first reviewer but never the second seeded reviewer the fixture edit also touched;
		// a missing version on that seed line would reintroduce the null-version NPE if the row were ever updated.
		assertThat(ReflectionTestUtils.getField(seeded, "version")).isEqualTo(0);
	}

	@Test
	void anotherSeededReviewer_updated_versionIncrementsFromSeededZero() {
		// given - the second reviewer the course_review.sql fixture inserts ("another-reviewer"), on a separate
		// INSERT line from the first reviewer, whose version this PR's fixture edit also seeds to 0
		final Reviewer seeded = reviewerRepository.findByUsername("another-reviewer");
		final Integer id = seeded.getId();

		// when - that seeded row is mutated and flushed
		ReflectionTestUtils.setField(seeded, "username", "another-reviewer-renamed");
		reviewerRepository.saveAndFlush(seeded);
		entityManager.clear();

		// then - the version increments cleanly from the seeded 0 to 1. anotherSeededReviewer_versionInitializedToZero
		// only reads that seed line; this proves updating it does not increment a null version (the NPE the fixture
		// edit guards against), the second-reviewer counterpart of reviewer_seededRowUpdated_versionIncrementsFromSeededZero.
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
	}

	@Test
	void reviewer_seededRowUpdated_versionIncrementsFromSeededZero() {
		// given - the reviewer row inserted by the fixture (version seeded to 0, not by a JPA insert).
		// The existing reviewer update test persists a fresh reviewer; this exercises the seeded row that
		// the fixture edit targets - without the seeded version = 0 the update would increment a null version.
		final Reviewer seeded = reviewerRepository.findByUsername("reviewer");
		final Integer id = seeded.getId();

		// when - the seeded reviewer is mutated and flushed
		ReflectionTestUtils.setField(seeded, "username", "reviewer-renamed");
		reviewerRepository.saveAndFlush(seeded);
		entityManager.clear();

		// then - the version increments cleanly from the seeded 0 to 1 instead of failing on a null version
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
	}

	@Test
	void reviewableCourse_seededRowUpdated_versionIncrementsFromSeededZero() {
		// given - the reviewable_course row inserted by the fixture (version seeded to 0). The existing
		// reviewable_course update tests persist a fresh row; this exercises the seeded row the fixture targets.
		final ReviewableCourse seeded = reviewableCourseRepository.findByOriginalCourseId(COURSE_UUID).orElseThrow();
		final Integer id = seeded.getId();

		// when - the seeded reviewable course is mutated and flushed
		ReflectionTestUtils.setField(seeded, "originalCourseId", UUID.randomUUID());
		reviewableCourseRepository.saveAndFlush(seeded);
		entityManager.clear();

		// then - the version increments cleanly from the seeded 0 to 1 instead of failing on a null version
		final ReviewableCourse reloaded = reviewableCourseRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
	}
}
