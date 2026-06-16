package com.educational.platform.course.reviews.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.UUID;

import javax.sql.DataSource;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.educational.platform.course.reviews.Comment;
import com.educational.platform.course.reviews.CourseRating;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;

import liquibase.integration.spring.SpringLiquibase;

/**
 * Verifies the {@code @Version} optimistic locking works against the <em>production-shaped</em> schema, where the
 * backing {@code version} column is the {@code BIGINT} created by the {@code db/course-reviews.yml} Liquibase
 * changeSets while the JPA-managed field is typed {@code Integer}.
 *
 * <p>{@link OptimisticLockingTest} runs against a Hibernate-generated schema (so its {@code version} column is an
 * {@code INTEGER} that happens to match the field type), and {@link CourseReviewsLiquibaseMigrationTest} exercises the
 * real migration but only through raw JDBC. Neither runs the JPA entities against the migrated {@code BIGINT} column, so
 * the PR's deliberate {@code Integer} field / {@code BIGINT} column mismatch was previously unverified end-to-end. This
 * slice closes that gap: Hibernate DDL is disabled ({@code ddl-auto=none}) and the schema is built by the actual
 * changelog, so the optimistic-locking behaviour is asserted through the Spring Data repositories on the same column
 * type production uses.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@Import(OptimisticLockingMigratedSchemaTest.LiquibaseSchemaConfig.class)
public class OptimisticLockingMigratedSchemaTest {

	@TestConfiguration
	static class LiquibaseSchemaConfig {

		// liquibase-core ships SpringLiquibase, but the (separate) Spring Boot Liquibase auto-configuration module is
		// not on this slice's classpath, so the changelog is applied explicitly against the slice datasource.
		@Bean
		SpringLiquibase courseReviewsLiquibase(DataSource dataSource) {
			final SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setDataSource(dataSource);
			liquibase.setChangeLog("classpath:db/course-reviews.yml");
			return liquibase;
		}
	}

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private CourseReviewRepository courseReviewRepository;

	@Autowired
	private ReviewerRepository reviewerRepository;

	@Test
	void precondition_versionColumnIsBigintFromTheMigration() throws Exception {
		// the whole point of this slice: the entities run against the BIGINT version column the changelog creates,
		// not the INTEGER one Hibernate would generate from the Integer field
		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "REVIEWER", "VERSION")) {
			assertThat(columns.next()).as("version column exists on the migrated reviewer table").isTrue();
			assertThat(columns.getString("TYPE_NAME")).isEqualTo("BIGINT");
		}
	}

	@Test
	void precondition_courseReviewVersionColumnIsBigintFromTheMigration() throws Exception {
		// companion to precondition_versionColumnIsBigintFromTheMigration, which pins only the reviewer column:
		// every CourseReview test in this slice runs against the migrated course_review table, so its version
		// column must be the BIGINT the changelog creates for those tests to exercise the real Integer/BIGINT mismatch
		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "COURSE_REVIEW", "VERSION")) {
			assertThat(columns.next()).as("version column exists on the migrated course_review table").isTrue();
			assertThat(columns.getString("TYPE_NAME")).isEqualTo("BIGINT");
		}
	}

	// --- Reviewer -----------------------------------------------------------

	@Test
	void reviewer_persistedAgainstBigintColumn_versionInitializedToZero() {
		// given/when - the Integer @Version field is written into the BIGINT column on insert
		final Reviewer reviewer = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer")));

		// then - it reads back as 0 despite the column being BIGINT
		assertThat(ReflectionTestUtils.getField(reviewer, "version")).isEqualTo(0);
	}

	@Test
	void reviewer_updatedAgainstBigintColumn_versionIncrementsAndPersists() {
		// given - a reviewer persisted at version 0
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();

		// when - it is updated and flushed
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(reviewer, "username", "migrated-reviewer-renamed");
		reviewerRepository.saveAndFlush(reviewer);

		// then - the increment round-trips through the BIGINT column
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
	}

	@Test
	void reviewer_persistedAgainstBigintColumn_versionZeroReadBackFromDatabase() {
		// given - a reviewer persisted with its Integer @Version written into the migrated BIGINT column
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();

		// when - the persistence context is cleared and the row reloaded from the migrated schema
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();

		// then - the initial version round-trips back out of the BIGINT column as 0. The existing insert
		// test only checks the in-memory entity returned by saveAndFlush; this confirms the value is
		// actually read back from the column on the insert path, not just the update path.
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(0);
	}

	@Test
	void reviewer_concurrentUpdateAgainstBigintColumn_throwsOptimisticLockingFailure() {
		// given - two instances reading the same migrated row at version 0
		final Integer id = reviewerRepository.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();
		final Reviewer stale = reviewerRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final Reviewer fresh = reviewerRepository.findById(id).orElseThrow();

		// and - the first writer wins, bumping the BIGINT version to 1
		ReflectionTestUtils.setField(fresh, "username", "first-wins");
		reviewerRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		ReflectionTestUtils.setField(stale, "username", "stale-loses");

		// then - the version guard rejects it just as it does on the Hibernate-generated schema
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewerRepository.saveAndFlush(stale));
	}

	@Test
	void reviewer_multipleSequentialUpdatesAgainstBigintColumn_versionIncrementsAndPersists() {
		// given - a reviewer persisted at version 0
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();

		// when - it is updated twice in a row, each update bumping the BIGINT version
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(reviewer, "username", "migrated-reviewer-renamed-once");
		reviewerRepository.saveAndFlush(reviewer);
		ReflectionTestUtils.setField(reviewer, "username", "migrated-reviewer-renamed-twice");
		reviewerRepository.saveAndFlush(reviewer);

		// then - both increments round-trip through the BIGINT column (version 2, not only the first bump).
		// The single-update test leaves repeated increments on the production-shaped column unverified.
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
	}

	@Test
	void reviewer_staleDeleteAgainstBigintColumn_throwsOptimisticLockingFailure() {
		// given - two instances reading the same migrated row at version 0
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();
		final Reviewer stale = reviewerRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final Reviewer fresh = reviewerRepository.findById(id).orElseThrow();

		// and - the first writer wins, bumping the BIGINT version to 1
		ReflectionTestUtils.setField(fresh, "username", "first-wins");
		reviewerRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to delete the row
		// then - the version guard rejects the delete on the production-shaped BIGINT column too, not only
		// updates. The migrated slice previously exercised only the update guard, never the delete guard.
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> {
					reviewerRepository.delete(stale);
					reviewerRepository.flush();
				});
	}

	@Test
	void reviewer_staleDeleteAgainstBigintColumn_failureIdentifiesEntityAndIsRootedInStaleVersion() {
		// given - two instances reading the same migrated row at version 0
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();
		final Reviewer stale = reviewerRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final Reviewer fresh = reviewerRepository.findById(id).orElseThrow();

		// and - the first writer wins, bumping the BIGINT version to 1
		ReflectionTestUtils.setField(fresh, "username", "first-wins");
		reviewerRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to delete the row
		// then - on the production-shaped BIGINT column the delete guard surfaces the full diagnostic contract
		// (version-specific subtype, conflicting entity/row, StaleObjectStateException root cause), the delete-guard
		// counterpart of reviewer_concurrentUpdateAgainstBigintColumn_failureIdentifies... The existing migrated
		// stale-delete test only asserts the generic OptimisticLockingFailureException supertype.
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
	void reviewer_currentDeleteAgainstBigintColumn_succeeds() {
		// given - a reviewer read at its current persisted version on the migrated schema
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();

		// when - it is deleted at its current version and flushed
		reviewerRepository.delete(reviewer);
		reviewerRepository.flush();

		// then - the success counterpart to the stale-delete rejection: the version guard does not block a
		// delete of a row read at its current version on the BIGINT column either
		entityManager.clear();
		assertThat(reviewerRepository.findById(id)).isEmpty();
	}

	@Test
	void reviewer_largeVersionWithinIntRangeAgainstBigintColumn_incrementsAndRoundTrips() {
		// given - a reviewer whose BIGINT version column already holds a large value. It is set directly via
		// SQL to simulate a long-lived, heavily updated row without performing two billion updates. The value
		// stays just inside Integer range, which is exactly the boundary the PR's Integer-field / BIGINT-column
		// mismatch hinges on: the column can hold far more than the Integer @Version field can read back.
		final long largeVersion = 2_000_000_000L; // < Integer.MAX_VALUE (2_147_483_647), comfortably inside BIGINT
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("large-version-reviewer"))).getId();
		entityManager.getEntityManager()
				.createNativeQuery("UPDATE reviewer SET version = ? WHERE id = ?")
				.setParameter(1, largeVersion)
				.setParameter(2, id)
				.executeUpdate();
		entityManager.clear();

		// when - the row is loaded (reading the large BIGINT value into the Integer field) and updated
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reviewer, "version")).isEqualTo((int) largeVersion);
		ReflectionTestUtils.setField(reviewer, "username", "large-version-reviewer-renamed");
		reviewerRepository.saveAndFlush(reviewer);

		// then - the increment round-trips back through the BIGINT column without overflow at the int boundary
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo((int) (largeVersion + 1));
	}

	@Test
	void reviewer_concurrentUpdateAgainstBigintColumn_failureIdentifiesEntityAndIsRootedInStaleVersion() {
		// given - two instances reading the same migrated row at version 0
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();
		final Reviewer stale = reviewerRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final Reviewer fresh = reviewerRepository.findById(id).orElseThrow();

		// and - the first writer wins, bumping the BIGINT version to 1
		ReflectionTestUtils.setField(fresh, "username", "first-wins");
		reviewerRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		ReflectionTestUtils.setField(stale, "username", "stale-loses");

		// then - on the production-shaped BIGINT column the conflict still surfaces as the version-specific
		// subtype that names the conflicting entity/row and is rooted in Hibernate's stale-version detection,
		// so callers can tell a version conflict apart from other data-access failures. OptimisticLockingTest
		// asserts this diagnostic contract on the Hibernate-generated INTEGER column; here it is pinned to the
		// migrated BIGINT column the existing migrated tests only checked the generic supertype against.
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewerRepository.saveAndFlush(stale))
				.satisfies(ex -> {
					assertThat(ex.getPersistentClassName()).isEqualTo(Reviewer.class.getName());
					assertThat(ex.getIdentifier()).isEqualTo(id);
				})
				.withRootCauseInstanceOf(StaleObjectStateException.class);
	}

	@Test
	void reviewer_concurrentUpdateAgainstBigintColumn_winnerPersistedAndStaleRejected() {
		// given - two instances reading the same migrated row at version 0
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();
		final Reviewer stale = reviewerRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final Reviewer fresh = reviewerRepository.findById(id).orElseThrow();

		// and - the first writer wins, persisting its username against the BIGINT-versioned row
		ReflectionTestUtils.setField(fresh, "username", "first-wins");
		reviewerRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// then - the winner's value is the one actually stored (the reviewer counterpart of
		// courseReview_concurrentUpdateAgainstBigintColumn_winnerPersistedAndStaleRejected, which had no
		// reviewer equivalent on the migrated schema)
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "username")).isEqualTo("first-wins");
		entityManager.clear();

		// when - the stale instance (still version 0) tries to overwrite with its own value
		ReflectionTestUtils.setField(stale, "username", "stale-loses");

		// then - it is rejected instead of silently overwriting the winner on the production-shaped column
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewerRepository.saveAndFlush(stale));
	}

	@Test
	void reviewer_concurrentUpdateAfterRefreshAgainstBigintColumn_succeedsAndVersionIncrements() {
		// given - the first writer wins, bumping the BIGINT version to 1
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();
		final Reviewer first = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(first, "username", "first-wins");
		reviewerRepository.saveAndFlush(first);
		entityManager.clear();

		// when - a second writer re-reads the current state (version 1) before updating, so it is not stale
		// and its update is accepted - the success counterpart to the stale-update rejection, here on the
		// migrated BIGINT column rather than the Hibernate-generated one
		final Reviewer second = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(second, "username", "second-also-wins");
		reviewerRepository.saveAndFlush(second);

		// then - the version advanced to 2 and the second writer's value round-trips through the BIGINT column
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
		assertThat(ReflectionTestUtils.getField(reloaded, "username")).isEqualTo("second-also-wins");
	}

	@Test
	void reviewer_staleUpdateAfterConcurrentDeleteAgainstBigintColumn_throwsOptimisticLockingFailure() {
		// given - two instances reading the same migrated row at version 0
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
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

		// then - the version guard rejects the update instead of resurrecting the deleted row on the BIGINT
		// column (the migrated slice previously covered stale-update-after-concurrent-update, not after-delete)
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewerRepository.saveAndFlush(stale));
	}

	@Test
	void reviewer_savedWithoutModificationAgainstBigintColumn_versionNotIncremented() {
		// given - a reviewer persisted at version 0 on the migrated schema
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();

		// when - it is re-saved and flushed without changing any field
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		reviewerRepository.saveAndFlush(reviewer);

		// then - with nothing dirty Hibernate issues no UPDATE, so the BIGINT version stays 0. Every migrated
		// update test flushes a real change; this pins the no-op path on the production-shaped column.
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(0);
	}

	@Test
	void reviewer_updatedWithSameUsernameAgainstBigintColumn_versionNotIncremented() {
		// given - a reviewer persisted at version 0 on the migrated schema
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();

		// when - its username is re-assigned to the value it already has and flushed
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(reviewer, "username", "migrated-reviewer");
		reviewerRepository.saveAndFlush(reviewer);

		// then - Hibernate's dirty check finds the value unchanged and issues no UPDATE, so the BIGINT version
		// stays 0. This is the value-equal no-op (distinct from re-saving an untouched instance): courseReview
		// has it on the migrated schema and reviewer has it on the Hibernate schema, but reviewer was the only
		// one missing it against the production-shaped BIGINT column.
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(0);
	}

	@Test
	void reviewer_updatingOneRowAgainstBigintColumn_otherRowsVersionUnchanged() {
		// given - two independently persisted reviewers on the migrated schema, both starting at version 0
		final Integer firstId = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer-one"))).getId();
		final Integer secondId = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer-two"))).getId();
		entityManager.clear();

		// when - only the first reviewer is updated
		final Reviewer first = reviewerRepository.findById(firstId).orElseThrow();
		ReflectionTestUtils.setField(first, "username", "migrated-reviewer-one-renamed");
		reviewerRepository.saveAndFlush(first);
		entityManager.clear();

		// then - the BIGINT version bump is isolated to the updated row; the untouched row stays at version 0.
		// The migrated slice previously asserted bumps only on the row under test, never that siblings stay put.
		final Reviewer reloadedFirst = reviewerRepository.findById(firstId).orElseThrow();
		final Reviewer reloadedSecond = reviewerRepository.findById(secondId).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedFirst, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedSecond, "version")).isEqualTo(0);
	}

	@Test
	void reviewer_concurrentUpdateAfterWinnerAdvancedMultipleVersionsAgainstBigintColumn_throwsOptimisticLockingFailure() {
		// given - a stale instance captured at version 0 on the migrated schema
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();
		final Reviewer stale = reviewerRepository.findById(id).orElseThrow();
		entityManager.detach(stale);

		// and - the winning writer advances the BIGINT version by more than one (0 -> 1 -> 2) before the stale save
		final Reviewer fresh = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(fresh, "username", "first-wins");
		reviewerRepository.saveAndFlush(fresh);
		ReflectionTestUtils.setField(fresh, "username", "first-wins-again");
		reviewerRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		ReflectionTestUtils.setField(stale, "username", "stale-loses");

		// then - the version guard rejects the stale write on the production-shaped BIGINT column regardless of how
		// far the row has advanced, not only at a gap of one (the migrated concurrent-update tests cover a gap of one)
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> reviewerRepository.saveAndFlush(stale));
	}

	@Test
	void reviewer_deleteAfterConcurrentDeleteAgainstBigintColumn_isNoOpAndRowRemainsDeleted() {
		// given - two instances reading the same migrated row at version 0
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();
		final Reviewer stale = reviewerRepository.findById(id).orElseThrow();
		entityManager.detach(stale);
		final Reviewer fresh = reviewerRepository.findById(id).orElseThrow();

		// and - the other writer deletes the row first
		reviewerRepository.delete(fresh);
		reviewerRepository.flush();
		entityManager.clear();

		// when - the stale instance tries to delete the row that is already gone
		// then - the delete is idempotent on the BIGINT column: no spurious optimistic-lock failure for a row that
		// no longer exists (the delete-path counterpart of reviewer_staleUpdateAfterConcurrentDelete*), row stays gone
		reviewerRepository.delete(stale);
		reviewerRepository.flush();
		entityManager.clear();
		assertThat(reviewerRepository.findById(id)).isEmpty();
	}

	// --- CourseReview (the headline aggregate, against its real FK-constrained table) --------------------

	@Test
	void courseReview_persistedAgainstBigintColumn_versionInitializedToZero() throws Exception {
		// given - a course review wired to the FK parents required by the migrated course_review table
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");

		// when - it is persisted, writing the Integer version into the BIGINT column
		final CourseReview saved = courseReviewRepository.saveAndFlush(review);

		// then
		assertThat(ReflectionTestUtils.getField(saved, "version")).isEqualTo(0);
	}

	@Test
	void courseReview_updatedAgainstBigintColumn_versionIncrementsAndPersists() throws Exception {
		// given - a persisted course review at version 0
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// when - it is updated through the domain method and flushed
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		reloaded.update(new UpdateCourseReviewCommand(uuid, 5.0, "updated comment"));
		courseReviewRepository.saveAndFlush(reloaded);

		// then - the increment round-trips through the BIGINT column
		entityManager.clear();
		final CourseReview afterReload = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(afterReload, "version")).isEqualTo(1);
	}

	@Test
	void courseReview_concurrentUpdateAgainstBigintColumn_throwsOptimisticLockingFailure() throws Exception {
		// given - two instances reading the same migrated row at version 0
		final CourseReview seed = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = seed.toIdentifier();
		courseReviewRepository.saveAndFlush(seed);
		entityManager.clear();
		final CourseReview stale = courseReviewRepository.findByUuid(uuid).orElseThrow();
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(uuid).orElseThrow();

		// and - the first writer wins, bumping the BIGINT version to 1
		fresh.update(new UpdateCourseReviewCommand(uuid, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		stale.update(new UpdateCourseReviewCommand(uuid, 1.0, "stale loses"));

		// then - the version guard rejects it just as it does on the Hibernate-generated schema
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> courseReviewRepository.saveAndFlush(stale));
	}

	@Test
	void courseReview_concurrentUpdateAgainstBigintColumn_winnerPersistedAndStaleRejected() throws Exception {
		// given - two instances reading the same migrated row at version 0
		final CourseReview seed = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = seed.toIdentifier();
		courseReviewRepository.saveAndFlush(seed);
		entityManager.clear();
		final CourseReview stale = courseReviewRepository.findByUuid(uuid).orElseThrow();
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(uuid).orElseThrow();

		// and - the first writer wins, persisting its rating and comment against the BIGINT-versioned row
		fresh.update(new UpdateCourseReviewCommand(uuid, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// then - the winner's values are the ones actually stored. The Hibernate-schema test asserts this
		// through listCourseReviews, but that query selects reviewable_course.original_course_id, which the
		// migrated reviewable_course table does not expose, so the row is reloaded through the entity instead.
		entityManager.clear();
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(((CourseRating) ReflectionTestUtils.getField(reloaded, "rating")).rating()).isEqualTo(5.0);
		assertThat(((Comment) ReflectionTestUtils.getField(reloaded, "comment")).comment()).isEqualTo("first wins");
		entityManager.clear();

		// when - the stale instance (still version 0) tries to overwrite with its own values
		stale.update(new UpdateCourseReviewCommand(uuid, 1.0, "stale loses"));

		// then - it is rejected instead of silently overwriting the winner on the production-shaped column,
		// directly exercising the PR's "instead of silently overwriting each other" contract against BIGINT
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> courseReviewRepository.saveAndFlush(stale));
	}

	@Test
	void courseReview_multipleSequentialUpdatesAgainstBigintColumn_versionIncrementsAndPersists() throws Exception {
		// given - a persisted course review at version 0
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// when - it is updated twice in a row through the domain method, each update bumping the BIGINT version
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		reloaded.update(new UpdateCourseReviewCommand(uuid, 5.0, "first update"));
		courseReviewRepository.saveAndFlush(reloaded);
		reloaded.update(new UpdateCourseReviewCommand(uuid, 3.0, "second update"));
		courseReviewRepository.saveAndFlush(reloaded);

		// then - both increments round-trip through the BIGINT column for the headline aggregate (version 2,
		// not only the first bump), which the single-update migrated test leaves unverified
		entityManager.clear();
		final CourseReview afterReload = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(afterReload, "version")).isEqualTo(2);
	}

	@Test
	void courseReview_staleDeleteAgainstBigintColumn_throwsOptimisticLockingFailure() throws Exception {
		// given - two instances reading the same migrated row at version 0
		final CourseReview seed = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = seed.toIdentifier();
		courseReviewRepository.saveAndFlush(seed);
		entityManager.clear();
		final CourseReview stale = courseReviewRepository.findByUuid(uuid).orElseThrow();
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(uuid).orElseThrow();

		// and - the first writer wins, bumping the BIGINT version to 1
		fresh.update(new UpdateCourseReviewCommand(uuid, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to delete the row
		// then - the version guard rejects the delete on the headline aggregate's production-shaped table too,
		// the delete-guard counterpart of the update-guard tests on the BIGINT column
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> {
					courseReviewRepository.delete(stale);
					courseReviewRepository.flush();
				});
	}

	@Test
	void courseReview_staleDeleteAgainstBigintColumn_failureIdentifiesEntityAndIsRootedInStaleVersion() throws Exception {
		// given - two instances reading the same migrated row at version 0
		final CourseReview seed = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = seed.toIdentifier();
		courseReviewRepository.saveAndFlush(seed);
		entityManager.clear();
		final CourseReview stale = courseReviewRepository.findByUuid(uuid).orElseThrow();
		final Object reviewId = ReflectionTestUtils.getField(stale, "id");
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(uuid).orElseThrow();

		// and - the first writer wins, bumping the BIGINT version to 1
		fresh.update(new UpdateCourseReviewCommand(uuid, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to delete the row
		// then - the delete guard surfaces the full diagnostic contract for the headline aggregate on the
		// production-shaped BIGINT column (version-specific subtype, conflicting entity/row, StaleObjectStateException
		// root cause), the delete-guard counterpart of courseReview_concurrentUpdateAgainstBigintColumn_failureIdentifies...
		// courseReview_staleDeleteAgainstBigintColumn_throwsOptimisticLockingFailure only asserts the generic supertype.
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
	void courseReview_persistedAgainstBigintColumn_versionZeroReadBackFromDatabase() throws Exception {
		// given - a course review persisted with its Integer @Version written into the migrated BIGINT column
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		courseReviewRepository.saveAndFlush(review);

		// when - the persistence context is cleared and the row reloaded from the migrated schema
		entityManager.clear();
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();

		// then - the initial version round-trips back out of the BIGINT column as 0 for the headline aggregate.
		// courseReview_persistedAgainstBigintColumn_versionInitializedToZero only checks the in-memory entity
		// returned by saveAndFlush; this confirms the value is actually read back from the column on the insert
		// path, mirroring reviewer_persistedAgainstBigintColumn_versionZeroReadBackFromDatabase.
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(0);
	}

	@Test
	void courseReview_currentDeleteAgainstBigintColumn_succeeds() throws Exception {
		// given - a course review read at its current persisted version on the migrated schema
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();
		final CourseReview current = courseReviewRepository.findByUuid(uuid).orElseThrow();

		// when - it is deleted at its current version and flushed
		courseReviewRepository.delete(current);
		courseReviewRepository.flush();

		// then - the success counterpart to courseReview_staleDeleteAgainstBigintColumn_throwsOptimisticLockingFailure:
		// the version guard does not block deleting a row read at its current version on the headline aggregate's
		// BIGINT-versioned table, mirroring reviewer_currentDeleteAgainstBigintColumn_succeeds.
		entityManager.clear();
		assertThat(courseReviewRepository.findByUuid(uuid)).isEmpty();
	}

	@Test
	void courseReview_largeVersionWithinIntRangeAgainstBigintColumn_incrementsAndRoundTrips() throws Exception {
		// given - a course review whose BIGINT version column already holds a large value, set directly via SQL
		// to simulate a long-lived, heavily updated row without performing two billion updates. The value stays
		// just inside Integer range - the boundary the PR's Integer-field / BIGINT-column mismatch hinges on -
		// and this exercises it for the headline aggregate, which the reviewer-only large-version test leaves
		// unverified on course_review.
		final long largeVersion = 2_000_000_000L; // < Integer.MAX_VALUE (2_147_483_647), comfortably inside BIGINT
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		final Object id = ReflectionTestUtils.getField(courseReviewRepository.saveAndFlush(review), "id");
		entityManager.getEntityManager()
				.createNativeQuery("UPDATE course_review SET version = ? WHERE id = ?")
				.setParameter(1, largeVersion)
				.setParameter(2, id)
				.executeUpdate();
		entityManager.clear();

		// when - the row is loaded (reading the large BIGINT value into the Integer field) and updated
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo((int) largeVersion);
		reloaded.update(new UpdateCourseReviewCommand(uuid, 5.0, "large-version updated"));
		courseReviewRepository.saveAndFlush(reloaded);

		// then - the increment round-trips back through the BIGINT column without overflow at the int boundary
		entityManager.clear();
		final CourseReview afterReload = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(afterReload, "version")).isEqualTo((int) (largeVersion + 1));
	}

	@Test
	void reviewer_versionExceedingIntegerRangeAgainstBigintColumn_failsFastOnRead() {
		// given - a reviewer whose BIGINT version column is forced one past Integer.MAX_VALUE. The value is set
		// directly via SQL because the Integer @Version field can never produce it itself - this is the far side
		// of the boundary reviewer_largeVersionWithinIntRangeAgainstBigintColumn_incrementsAndRoundTrips stops
		// just inside, where the BIGINT column outgrows what the Integer field can represent.
		final long overIntVersion = Integer.MAX_VALUE + 1L; // 2_147_483_648: holds in BIGINT, overflows int
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("over-int-version-reviewer"))).getId();
		entityManager.getEntityManager()
				.createNativeQuery("UPDATE reviewer SET version = ? WHERE id = ?")
				.setParameter(1, overIntVersion)
				.setParameter(2, id)
				.executeUpdate();
		entityManager.clear();

		// then - the value really is stored in the BIGINT column (it is not clamped on write) ...
		final Number stored = (Number) entityManager.getEntityManager()
				.createNativeQuery("SELECT version FROM reviewer WHERE id = ?")
				.setParameter(1, id)
				.getSingleResult();
		assertThat(stored.longValue()).isEqualTo(overIntVersion);

		// ... but loading it through the entity fails fast instead of silently truncating the over-int value
		// into the Integer @Version field, so a row can never be read back with a corrupted (overflowed)
		// version. This documents the limit of the PR's deliberate Integer-field / BIGINT-column mismatch.
		assertThatExceptionOfType(DataIntegrityViolationException.class)
				.isThrownBy(() -> reviewerRepository.findById(id));
	}

	@Test
	void courseReview_versionExceedingIntegerRangeAgainstBigintColumn_failsFastOnRead() throws Exception {
		// given - a course review whose BIGINT version column is forced one past Integer.MAX_VALUE, set directly
		// via SQL. Same boundary as the reviewer case but for the headline aggregate against its real
		// FK-constrained table, which courseReview_largeVersionWithinIntRangeAgainstBigintColumn stops just inside.
		final long overIntVersion = Integer.MAX_VALUE + 1L; // 2_147_483_648: holds in BIGINT, overflows int
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		final Object id = ReflectionTestUtils.getField(courseReviewRepository.saveAndFlush(review), "id");
		entityManager.getEntityManager()
				.createNativeQuery("UPDATE course_review SET version = ? WHERE id = ?")
				.setParameter(1, overIntVersion)
				.setParameter(2, id)
				.executeUpdate();
		entityManager.clear();

		// then - the value really is stored in the BIGINT column ...
		final Number stored = (Number) entityManager.getEntityManager()
				.createNativeQuery("SELECT version FROM course_review WHERE id = ?")
				.setParameter(1, id)
				.getSingleResult();
		assertThat(stored.longValue()).isEqualTo(overIntVersion);

		// ... but loading the aggregate through the entity fails fast rather than truncating the over-int
		// version into the Integer @Version field, the headline-aggregate counterpart of the reviewer boundary
		assertThatExceptionOfType(DataIntegrityViolationException.class)
				.isThrownBy(() -> courseReviewRepository.findByUuid(uuid));
	}

	@Test
	void courseReview_concurrentUpdateAgainstBigintColumn_failureIdentifiesEntityAndIsRootedInStaleVersion() throws Exception {
		// given - two instances reading the same migrated row at version 0
		final CourseReview seed = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = seed.toIdentifier();
		courseReviewRepository.saveAndFlush(seed);
		entityManager.clear();
		final CourseReview stale = courseReviewRepository.findByUuid(uuid).orElseThrow();
		final Object reviewId = ReflectionTestUtils.getField(stale, "id");
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(uuid).orElseThrow();

		// and - the first writer wins, bumping the BIGINT version to 1
		fresh.update(new UpdateCourseReviewCommand(uuid, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		stale.update(new UpdateCourseReviewCommand(uuid, 1.0, "stale loses"));

		// then - the conflict surfaces as the version-specific subtype, names the conflicting entity/row, and is
		// rooted in Hibernate's stale-version detection on the production-shaped BIGINT column (the headline
		// aggregate's counterpart of the reviewer diagnostic test; the existing migrated concurrent-update test
		// only asserts the generic OptimisticLockingFailureException supertype)
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> courseReviewRepository.saveAndFlush(stale))
				.satisfies(ex -> {
					assertThat(ex.getPersistentClassName()).isEqualTo(CourseReview.class.getName());
					assertThat(ex.getIdentifier()).isEqualTo(reviewId);
				})
				.withRootCauseInstanceOf(StaleObjectStateException.class);
	}

	@Test
	void courseReview_concurrentUpdateAfterRefreshAgainstBigintColumn_succeedsAndVersionIncrements() throws Exception {
		// given - the first writer wins, bumping the BIGINT version to 1
		final CourseReview seed = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = seed.toIdentifier();
		courseReviewRepository.saveAndFlush(seed);
		entityManager.clear();
		final CourseReview first = courseReviewRepository.findByUuid(uuid).orElseThrow();
		first.update(new UpdateCourseReviewCommand(uuid, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(first);
		entityManager.clear();

		// when - a second writer re-reads the current state (version 1) before updating, so it is not stale and
		// its update is accepted - the success counterpart to the stale-update rejection on the migrated column
		final CourseReview second = courseReviewRepository.findByUuid(uuid).orElseThrow();
		second.update(new UpdateCourseReviewCommand(uuid, 2.0, "second also wins"));
		courseReviewRepository.saveAndFlush(second);

		// then - the version advanced to 2 and the second writer's values round-trip through the BIGINT column.
		// The row is reloaded through the entity because listCourseReviews selects reviewable_course.original_course_id,
		// which the migrated reviewable_course table does not expose.
		entityManager.clear();
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
		assertThat(((CourseRating) ReflectionTestUtils.getField(reloaded, "rating")).rating()).isEqualTo(2.0);
		assertThat(((Comment) ReflectionTestUtils.getField(reloaded, "comment")).comment()).isEqualTo("second also wins");
	}

	@Test
	void courseReview_staleUpdateAfterConcurrentDeleteAgainstBigintColumn_throwsOptimisticLockingFailure() throws Exception {
		// given - two instances reading the same migrated row at version 0
		final CourseReview seed = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = seed.toIdentifier();
		courseReviewRepository.saveAndFlush(seed);
		entityManager.clear();
		final CourseReview stale = courseReviewRepository.findByUuid(uuid).orElseThrow();
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(uuid).orElseThrow();

		// and - the other writer deletes the row out from under the stale instance
		courseReviewRepository.delete(fresh);
		courseReviewRepository.flush();
		entityManager.clear();

		// when - the stale instance (still version 0) tries to update the now-deleted row
		stale.update(new UpdateCourseReviewCommand(uuid, 1.0, "stale loses"));

		// then - the version guard rejects the update instead of resurrecting the deleted row on the BIGINT
		// column for the headline aggregate too
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> courseReviewRepository.saveAndFlush(stale));
	}

	@Test
	void courseReview_savedWithoutModificationAgainstBigintColumn_versionNotIncremented() throws Exception {
		// given - a course review persisted at version 0 on the migrated schema
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// when - it is re-saved and flushed without changing any field
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		courseReviewRepository.saveAndFlush(reloaded);

		// then - with nothing dirty Hibernate issues no UPDATE, so the BIGINT version stays 0
		entityManager.clear();
		final CourseReview afterReload = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(afterReload, "version")).isEqualTo(0);
	}

	@Test
	void courseReview_updatedWithSameValuesAgainstBigintColumn_versionNotIncremented() throws Exception {
		// given - a course review persisted at version 0 (rating 4.0, comment "comment")
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// when - it is "updated" with the values it already has and flushed
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		reloaded.update(new UpdateCourseReviewCommand(uuid, 4.0, "comment"));
		courseReviewRepository.saveAndFlush(reloaded);

		// then - the embeddable rating/comment are equal by value, so Hibernate issues no UPDATE and the BIGINT
		// version stays 0 (the migrated counterpart of courseReview_updatedWithSameValues on the Hibernate schema)
		entityManager.clear();
		final CourseReview afterReload = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(afterReload, "version")).isEqualTo(0);
	}

	@Test
	void courseReview_updatingOneReviewAgainstBigintColumn_otherReviewsVersionUnchanged() throws Exception {
		// given - two independently persisted course reviews on the migrated schema, both starting at version 0
		final CourseReview firstSeed = newCourseReviewForMigratedSchema(4.0, "first review");
		final UUID firstUuid = firstSeed.toIdentifier();
		courseReviewRepository.saveAndFlush(firstSeed);
		final CourseReview secondSeed = newCourseReviewForMigratedSchema(3.0, "second review");
		final UUID secondUuid = secondSeed.toIdentifier();
		courseReviewRepository.saveAndFlush(secondSeed);
		entityManager.clear();

		// when - only the second review is updated
		final CourseReview toUpdate = courseReviewRepository.findByUuid(secondUuid).orElseThrow();
		toUpdate.update(new UpdateCourseReviewCommand(secondUuid, 5.0, "second review updated"));
		courseReviewRepository.saveAndFlush(toUpdate);
		entityManager.clear();

		// then - the BIGINT version bump is isolated to the updated row; the untouched review stays at version 0
		final CourseReview reloadedFirst = courseReviewRepository.findByUuid(firstUuid).orElseThrow();
		final CourseReview reloadedSecond = courseReviewRepository.findByUuid(secondUuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedSecond, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedFirst, "version")).isEqualTo(0);
	}

	@Test
	void courseReview_concurrentUpdateAfterWinnerAdvancedMultipleVersionsAgainstBigintColumn_throwsOptimisticLockingFailure() throws Exception {
		// given - a stale instance captured at version 0 on the migrated schema
		final CourseReview seed = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = seed.toIdentifier();
		courseReviewRepository.saveAndFlush(seed);
		entityManager.clear();
		final CourseReview stale = courseReviewRepository.findByUuid(uuid).orElseThrow();
		entityManager.detach(stale);

		// and - the winning writer advances the BIGINT version by more than one (0 -> 1 -> 2) before the stale save,
		// so the gap between the stale version and the persisted version is greater than one
		final CourseReview fresh = courseReviewRepository.findByUuid(uuid).orElseThrow();
		fresh.update(new UpdateCourseReviewCommand(uuid, 5.0, "first wins"));
		courseReviewRepository.saveAndFlush(fresh);
		fresh.update(new UpdateCourseReviewCommand(uuid, 4.5, "first wins again"));
		courseReviewRepository.saveAndFlush(fresh);
		entityManager.detach(fresh);

		// when - the stale instance (still version 0) tries to update
		stale.update(new UpdateCourseReviewCommand(uuid, 1.0, "stale loses"));

		// then - the version guard rejects the stale write on the BIGINT column regardless of how far the row has
		// advanced, not only when it is exactly one version behind (the migrated concurrent-update tests use a gap of one)
		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> courseReviewRepository.saveAndFlush(stale));
	}

	@Test
	void courseReview_deleteAfterConcurrentDeleteAgainstBigintColumn_isNoOpAndRowRemainsDeleted() throws Exception {
		// given - two instances reading the same migrated row at version 0
		final CourseReview seed = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = seed.toIdentifier();
		courseReviewRepository.saveAndFlush(seed);
		entityManager.clear();
		final CourseReview stale = courseReviewRepository.findByUuid(uuid).orElseThrow();
		entityManager.detach(stale);
		final CourseReview fresh = courseReviewRepository.findByUuid(uuid).orElseThrow();

		// and - the other writer deletes the row first
		courseReviewRepository.delete(fresh);
		courseReviewRepository.flush();
		entityManager.clear();

		// when - the stale instance tries to delete the row that is already gone
		// then - the delete is idempotent on the BIGINT column: no spurious optimistic-lock failure for a row that
		// no longer exists (the delete-path counterpart of courseReview_staleUpdateAfterConcurrentDelete*), row stays gone
		courseReviewRepository.delete(stale);
		courseReviewRepository.flush();
		entityManager.clear();
		assertThat(courseReviewRepository.findByUuid(uuid)).isEmpty();
	}

	/**
	 * Builds a transient {@link CourseReview} bound to freshly created reviewer/reviewable-course FK parents.
	 *
	 * <p>The domain constructor is package-private and the {@code reviewable_course} table cannot be populated through
	 * its entity on this schema (its entity maps {@code original_course_id}, while the migrated table exposes
	 * {@code uuid}), so the course parent is inserted with native SQL and the review is built reflectively.
	 */
	private CourseReview newCourseReviewForMigratedSchema(double rating, String comment) throws Exception {
		final Integer reviewerId = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("cr-reviewer-" + UUID.randomUUID()))).getId();
		entityManager.getEntityManager()
				.createNativeQuery("INSERT INTO reviewable_course (uuid) VALUES (?)")
				.setParameter(1, UUID.randomUUID())
				.executeUpdate();
		final Number courseId = (Number) entityManager.getEntityManager()
				.createNativeQuery("SELECT MAX(id) FROM reviewable_course")
				.getSingleResult();

		final Constructor<CourseReview> constructor = CourseReview.class
				.getDeclaredConstructor(ReviewCourseCommand.class, Integer.class, Integer.class);
		constructor.setAccessible(true);
		return constructor.newInstance(
				new ReviewCourseCommand(UUID.randomUUID(), rating, comment), courseId.intValue(), reviewerId);
	}
}
