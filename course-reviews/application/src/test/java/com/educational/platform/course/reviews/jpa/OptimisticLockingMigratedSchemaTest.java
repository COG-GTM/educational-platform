package com.educational.platform.course.reviews.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.UUID;

import javax.sql.DataSource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

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
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommandHandler;
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
	void reviewer_updatedAgainstBigintColumn_newUsernamePersistedToDatabase() {
		// given - a reviewer persisted at version 0, then renamed and flushed against the migrated schema
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("migrated-reviewer"))).getId();
		entityManager.clear();
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		ReflectionTestUtils.setField(reviewer, "username", "migrated-reviewer-renamed");
		reviewerRepository.saveAndFlush(reviewer);

		// when - the persistence context is cleared and the row reloaded from the migrated schema
		entityManager.clear();
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();

		// then - a plain single update wrote the new field value alongside the version bump through the BIGINT
		// column, not the version alone. reviewer_updatedAgainstBigintColumn_versionIncrementsAndPersists pins
		// only the version on this path, so the business-value round-trip on a single update against the
		// production-shaped column was otherwise asserted only by the concurrent winner-persisted test - the
		// Hibernate slice keeps reviewer_updated_newUsernamePersistedToDatabase as its standalone counterpart.
		assertThat(ReflectionTestUtils.getField(reloaded, "username")).isEqualTo("migrated-reviewer-renamed");
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
	void courseReview_updatedAgainstBigintColumn_newRatingAndCommentPersistedToDatabase() throws Exception {
		// given - a persisted course review at version 0 on the migrated schema
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// when - it is updated once through the domain method and flushed
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		reloaded.update(new UpdateCourseReviewCommand(uuid, 5.0, "updated comment"));
		courseReviewRepository.saveAndFlush(reloaded);

		// and - the persistence context is cleared and the row reloaded through the entity (listCourseReviews
		// selects reviewable_course.original_course_id, which the migrated reviewable_course table does not expose)
		entityManager.clear();
		final CourseReview afterReload = courseReviewRepository.findByUuid(uuid).orElseThrow();

		// then - a plain single update persisted the new field values alongside the version bump through the BIGINT
		// column, not only the version. courseReview_updatedAgainstBigintColumn_versionIncrementsAndPersists pins
		// only the version on this path for the headline aggregate, so the business-value round-trip on a single
		// update against the production-shaped column was otherwise asserted only by the concurrent winner-persisted
		// test - the Hibernate slice keeps courseReview_updated_newRatingAndCommentPersistedToDatabase standalone.
		assertThat(((CourseRating) ReflectionTestUtils.getField(afterReload, "rating")).rating()).isEqualTo(5.0);
		assertThat(((Comment) ReflectionTestUtils.getField(afterReload, "comment")).comment()).isEqualTo("updated comment");
		assertThat(ReflectionTestUtils.getField(afterReload, "version")).isEqualTo(1);
	}

	@Test
	void courseReview_partialUpdateChangingOnlyCommentAgainstBigintColumn_versionIncrementedAndCommentPersisted() throws Exception {
		// given - a persisted course review at version 0 on the migrated schema (rating 4.0, comment "comment")
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// when - it is updated keeping the same rating (4.0) but a new comment, so only one mapped field changes.
		// Every other migrated update test flushes a change to both fields; the Hibernate slice keeps the single-
		// dirty-field bump (courseReview_partialUpdateChangingOnlyComment...) standalone, so the production-shaped
		// BIGINT column had no single-field-bump coverage at all.
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		reloaded.update(new UpdateCourseReviewCommand(uuid, 4.0, "only the comment changed"));
		courseReviewRepository.saveAndFlush(reloaded);

		// then - a single dirty field is enough to bump the BIGINT version, the changed comment is persisted and the
		// untouched rating survives. The row is reloaded through the entity (listCourseReviews selects
		// reviewable_course.original_course_id, which the migrated reviewable_course table does not expose).
		entityManager.clear();
		final CourseReview afterReload = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(afterReload, "version")).isEqualTo(1);
		assertThat(((CourseRating) ReflectionTestUtils.getField(afterReload, "rating")).rating()).isEqualTo(4.0);
		assertThat(((Comment) ReflectionTestUtils.getField(afterReload, "comment")).comment()).isEqualTo("only the comment changed");
	}

	@Test
	void courseReview_partialUpdateChangingOnlyRatingAgainstBigintColumn_versionIncrementedAndRatingPersisted() throws Exception {
		// given - a persisted course review at version 0 on the migrated schema (rating 4.0, comment "comment")
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// when - it is updated keeping the same comment ("comment") but a new rating, exercising the rating-only
		// single-dirty-field direction against the BIGINT column (the comment-only direction is covered above)
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		reloaded.update(new UpdateCourseReviewCommand(uuid, 5.0, "comment"));
		courseReviewRepository.saveAndFlush(reloaded);

		// then - changing only the rating bumps the BIGINT version, the new rating round-trips and the untouched
		// comment survives
		entityManager.clear();
		final CourseReview afterReload = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(afterReload, "version")).isEqualTo(1);
		assertThat(((CourseRating) ReflectionTestUtils.getField(afterReload, "rating")).rating()).isEqualTo(5.0);
		assertThat(((Comment) ReflectionTestUtils.getField(afterReload, "comment")).comment()).isEqualTo("comment");
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
	void courseReview_winnerCommittedViaUpdateHandlerAgainstBigintColumn_staleRepositorySaveRejected() throws Exception {
		// given - a stale instance captured at version 0 on the migrated schema, detached before the winning write
		final CourseReview seed = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = seed.toIdentifier();
		courseReviewRepository.saveAndFlush(seed);
		entityManager.clear();
		final CourseReview stale = courseReviewRepository.findByUuid(uuid).orElseThrow();
		final Object reviewId = ReflectionTestUtils.getField(stale, "id");
		entityManager.detach(stale);

		// and - the winning update is committed through the production update handler (read-modify-save), bumping the
		// BIGINT version to 1. courseReview_concurrentUpdateAgainstBigintColumn_winnerPersistedAndStaleRejected commits
		// the winner straight through the repository, and courseReview_winnerCommittedViaUpdateHandler_staleRepositorySaveRejected
		// routes it through the real use case but against the Hibernate-generated INTEGER column. Only here does the
		// production update use case meet the production-shaped BIGINT column on the conflict path.
		final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		final UpdateCourseReviewCommandHandler handler =
				new UpdateCourseReviewCommandHandler(validator, courseReviewRepository);
		handler.handle(new UpdateCourseReviewCommand(uuid, 5.0, "handler wins"));
		entityManager.flush();
		entityManager.clear();

		// then - the handler's values are the ones actually stored (reloaded through the entity, since listCourseReviews
		// selects reviewable_course.original_course_id, which the migrated reviewable_course table does not expose)
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(((CourseRating) ReflectionTestUtils.getField(reloaded, "rating")).rating()).isEqualTo(5.0);
		assertThat(((Comment) ReflectionTestUtils.getField(reloaded, "comment")).comment()).isEqualTo("handler wins");
		entityManager.clear();

		// when - the stale instance (still version 0) tries to overwrite the handler's committed change
		stale.update(new UpdateCourseReviewCommand(uuid, 1.0, "stale loses"));

		// then - the loser is rejected with the version-specific failure (entity + row identified, rooted in stale-version
		// detection) instead of silently overwriting the value the production handler committed against the BIGINT column
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> courseReviewRepository.saveAndFlush(stale))
				.satisfies(ex -> {
					assertThat(ex.getPersistentClassName()).isEqualTo(CourseReview.class.getName());
					assertThat(ex.getIdentifier()).isEqualTo(reviewId);
				})
				.withRootCauseInstanceOf(StaleObjectStateException.class);
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
	void reviewer_versionAtIntegerMaxValueAgainstBigintColumn_readsBackAtBoundary() {
		// given - a reviewer whose BIGINT version column is forced to exactly Integer.MAX_VALUE, the largest
		// value the Integer @Version field can represent. Set directly via SQL because ordinary increments can
		// never reach it. This is the on-point of the boundary that the two existing tests bracket but never
		// land on: reviewer_largeVersionWithinIntRangeAgainstBigintColumn (2_000_000_000) stops just below it and
		// reviewer_versionExceedingIntegerRangeAgainstBigintColumn_failsFastOnRead (MAX + 1) steps just past it.
		final long maxIntVersion = Integer.MAX_VALUE; // 2_147_483_647: the largest value the Integer field holds
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("max-version-reviewer"))).getId();
		entityManager.getEntityManager()
				.createNativeQuery("UPDATE reviewer SET version = ? WHERE id = ?")
				.setParameter(1, maxIntVersion)
				.setParameter(2, id)
				.executeUpdate();
		entityManager.clear();

		// then - the boundary value still round-trips out of the BIGINT column into the Integer @Version field
		// intact, so the read fails fast only once the column outgrows Integer range (MAX + 1), not at the edge.
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void reviewer_versionAtIntegerMaxValueAgainstBigintColumn_incrementWrapsToIntegerMinValue() {
		// given - a reviewer whose BIGINT version column is forced to exactly Integer.MAX_VALUE, the largest value
		// the Integer @Version field can represent, set directly via SQL because ordinary increments can never reach
		// it. This is the on-point boundary the three existing large-version tests bracket but never increment *from*:
		// reviewer_largeVersionWithinIntRangeAgainstBigintColumn_incrementsAndRoundTrips updates from just below it,
		// reviewer_versionAtIntegerMaxValueAgainstBigintColumn_readsBackAtBoundary only reads it, and
		// reviewer_versionExceedingIntegerRangeAgainstBigintColumn_failsFastOnRead steps one past it on read.
		final long maxIntVersion = Integer.MAX_VALUE; // 2_147_483_647: the largest value the Integer field holds
		final Integer id = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("max-increment-reviewer"))).getId();
		entityManager.getEntityManager()
				.createNativeQuery("UPDATE reviewer SET version = ? WHERE id = ?")
				.setParameter(1, maxIntVersion)
				.setParameter(2, id)
				.executeUpdate();
		entityManager.clear();

		// when - the boundary row is loaded (reading MAX into the Integer field) and updated, so Hibernate computes
		// the next optimistic-lock version as Integer.MAX_VALUE + 1 and writes it back to the BIGINT column
		final Reviewer reviewer = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reviewer, "version")).isEqualTo(Integer.MAX_VALUE);
		ReflectionTestUtils.setField(reviewer, "username", "max-increment-reviewer-renamed");
		reviewerRepository.saveAndFlush(reviewer);

		// then - incrementing *from* the boundary does not fail fast; the Integer @Version silently wraps on int
		// overflow to Integer.MIN_VALUE and that wrapped (negative) value is stored verbatim in the BIGINT column,
		// neither clamped nor rejected, and reads back intact alongside the persisted business change. This
		// documents the upper limit of the PR's deliberate Integer-field / BIGINT-column choice: the failsFastOnRead
		// test pins the safe failure for a column value already beyond int range, while this pins that an in-range
		// boundary value increments by wrapping rather than erroring.
		entityManager.clear();
		final Number stored = (Number) entityManager.getEntityManager()
				.createNativeQuery("SELECT version FROM reviewer WHERE id = ?")
				.setParameter(1, id)
				.getSingleResult();
		assertThat(stored.longValue()).isEqualTo((long) Integer.MIN_VALUE);
		final Reviewer reloaded = reviewerRepository.findById(id).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(Integer.MIN_VALUE);
		assertThat(ReflectionTestUtils.getField(reloaded, "username")).isEqualTo("max-increment-reviewer-renamed");
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
	void courseReview_versionAtIntegerMaxValueAgainstBigintColumn_readsBackAtBoundary() throws Exception {
		// given - a course review whose BIGINT version column is forced to exactly Integer.MAX_VALUE, set directly
		// via SQL. Same on-point boundary as the reviewer case but for the headline aggregate against its real
		// FK-constrained table: courseReview_largeVersionWithinIntRangeAgainstBigintColumn stops just below it and
		// courseReview_versionExceedingIntegerRangeAgainstBigintColumn_failsFastOnRead steps just past it.
		final long maxIntVersion = Integer.MAX_VALUE; // 2_147_483_647: the largest value the Integer field holds
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		final Object id = ReflectionTestUtils.getField(courseReviewRepository.saveAndFlush(review), "id");
		entityManager.getEntityManager()
				.createNativeQuery("UPDATE course_review SET version = ? WHERE id = ?")
				.setParameter(1, maxIntVersion)
				.setParameter(2, id)
				.executeUpdate();
		entityManager.clear();

		// then - the boundary value round-trips out of the BIGINT column into the Integer @Version field intact
		// for the headline aggregate, so the read only fails fast once the column outgrows Integer range (MAX + 1).
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void courseReview_versionAtIntegerMaxValueAgainstBigintColumn_incrementWrapsToIntegerMinValue() throws Exception {
		// given - a course review whose BIGINT version column is forced to exactly Integer.MAX_VALUE, set directly
		// via SQL. Same on-point boundary as the reviewer case but for the headline aggregate against its real
		// FK-constrained table: courseReview_largeVersionWithinIntRangeAgainstBigintColumn updates from just below it,
		// courseReview_versionAtIntegerMaxValueAgainstBigintColumn_readsBackAtBoundary only reads it, and
		// courseReview_versionExceedingIntegerRangeAgainstBigintColumn_failsFastOnRead steps one past it on read.
		final long maxIntVersion = Integer.MAX_VALUE; // 2_147_483_647: the largest value the Integer field holds
		final CourseReview review = newCourseReviewForMigratedSchema(4.0, "comment");
		final UUID uuid = review.toIdentifier();
		final Object id = ReflectionTestUtils.getField(courseReviewRepository.saveAndFlush(review), "id");
		entityManager.getEntityManager()
				.createNativeQuery("UPDATE course_review SET version = ? WHERE id = ?")
				.setParameter(1, maxIntVersion)
				.setParameter(2, id)
				.executeUpdate();
		entityManager.clear();

		// when - the boundary row is loaded (reading MAX into the Integer field) and updated, so Hibernate computes
		// the next optimistic-lock version as Integer.MAX_VALUE + 1 and writes it back to the BIGINT column
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(Integer.MAX_VALUE);
		reloaded.update(new UpdateCourseReviewCommand(uuid, 5.0, "max-version updated"));
		courseReviewRepository.saveAndFlush(reloaded);

		// then - incrementing *from* the boundary does not fail fast for the headline aggregate either; the Integer
		// @Version silently wraps on int overflow to Integer.MIN_VALUE and that wrapped (negative) value is stored
		// verbatim in the BIGINT column and reads back intact alongside the persisted rating/comment change. The
		// headline-aggregate counterpart of reviewer_versionAtIntegerMaxValueAgainstBigintColumn_incrementWrapsToIntegerMinValue,
		// completing the boundary set the failsFastOnRead / readsBackAtBoundary tests leave open on the update path.
		entityManager.clear();
		final Number stored = (Number) entityManager.getEntityManager()
				.createNativeQuery("SELECT version FROM course_review WHERE id = ?")
				.setParameter(1, id)
				.getSingleResult();
		assertThat(stored.longValue()).isEqualTo((long) Integer.MIN_VALUE);
		final CourseReview afterReload = courseReviewRepository.findByUuid(uuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(afterReload, "version")).isEqualTo(Integer.MIN_VALUE);
		assertThat(afterReload)
				.hasFieldOrPropertyWithValue("rating", new CourseRating(5.0))
				.hasFieldOrPropertyWithValue("comment", new Comment("max-version updated"));
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
	void courseReview_updatedAgainstBigintColumn_linkedParentVersionsUnchanged() throws Exception {
		// given - a course review wired to freshly created reviewer/reviewable_course FK parents on the migrated
		// (BIGINT) schema, all at version 0. The parents are built inline (mirroring newCourseReviewForMigratedSchema)
		// so their ids can be captured and re-read after the update.
		final Integer reviewerId = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("cross-aggregate-reviewer-" + UUID.randomUUID()))).getId();
		entityManager.getEntityManager()
				.createNativeQuery("INSERT INTO reviewable_course (uuid) VALUES (?)")
				.setParameter(1, UUID.randomUUID())
				.executeUpdate();
		final Number reviewableCourseId = (Number) entityManager.getEntityManager()
				.createNativeQuery("SELECT MAX(id) FROM reviewable_course")
				.getSingleResult();
		final Constructor<CourseReview> constructor = CourseReview.class
				.getDeclaredConstructor(ReviewCourseCommand.class, Integer.class, Integer.class);
		constructor.setAccessible(true);
		final CourseReview review = constructor.newInstance(
				new ReviewCourseCommand(UUID.randomUUID(), 4.0, "comment"), reviewableCourseId.intValue(), reviewerId);
		final UUID uuid = review.toIdentifier();
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// when - only the course review is updated
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		reloaded.update(new UpdateCourseReviewCommand(uuid, 5.0, "updated comment"));
		courseReviewRepository.saveAndFlush(reloaded);
		entityManager.clear();

		// then - the BIGINT version bump is isolated to the course_review aggregate; the referenced reviewer and
		// reviewable_course rows are separate aggregates linked only by FK id, so their versions stay 0 on the
		// production-shaped schema. (reviewable_course is read via native SQL because its entity maps
		// original_course_id while the migrated table exposes uuid, as documented on newCourseReviewForMigratedSchema.)
		final CourseReview afterReload = courseReviewRepository.findByUuid(uuid).orElseThrow();
		final Reviewer reloadedReviewer = reviewerRepository.findById(reviewerId).orElseThrow();
		final Number reviewableCourseVersion = (Number) entityManager.getEntityManager()
				.createNativeQuery("SELECT version FROM reviewable_course WHERE id = ?")
				.setParameter(1, reviewableCourseId.intValue())
				.getSingleResult();
		assertThat(ReflectionTestUtils.getField(afterReload, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedReviewer, "version")).isEqualTo(0);
		assertThat(reviewableCourseVersion.longValue()).isZero();
	}

	@Test
	void courseReview_createdAgainstBigintColumn_linkedParentVersionsUnchanged() throws Exception {
		// given - freshly created reviewer/reviewable_course FK parents on the migrated (BIGINT) schema, both at
		// version 0. reviewable_course is inserted with native SQL because its entity maps original_course_id while
		// the migrated table exposes uuid (as documented on newCourseReviewForMigratedSchema).
		final Integer reviewerId = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("insert-isolation-reviewer-" + UUID.randomUUID()))).getId();
		entityManager.getEntityManager()
				.createNativeQuery("INSERT INTO reviewable_course (uuid) VALUES (?)")
				.setParameter(1, UUID.randomUUID())
				.executeUpdate();
		final Number reviewableCourseId = (Number) entityManager.getEntityManager()
				.createNativeQuery("SELECT MAX(id) FROM reviewable_course")
				.getSingleResult();
		final Constructor<CourseReview> constructor = CourseReview.class
				.getDeclaredConstructor(ReviewCourseCommand.class, Integer.class, Integer.class);
		constructor.setAccessible(true);
		final CourseReview review = constructor.newInstance(
				new ReviewCourseCommand(UUID.randomUUID(), 4.0, "comment"), reviewableCourseId.intValue(), reviewerId);
		final UUID uuid = review.toIdentifier();

		// when - the new review is inserted against the migrated course_review table, establishing the FK link
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// then - inserting the child writes its own version 0 into the BIGINT column and leaves the referenced parents
		// untouched (no JPA cascade), so their BIGINT versions stay 0. The migrated slice only asserted parent
		// isolation on the update path (courseReview_updatedAgainstBigintColumn_linkedParentVersionsUnchanged); the
		// insert path that first establishes the link was unverified on the production-shaped schema.
		final CourseReview reloaded = courseReviewRepository.findByUuid(uuid).orElseThrow();
		final Reviewer reloadedReviewer = reviewerRepository.findById(reviewerId).orElseThrow();
		final Number reviewableCourseVersion = (Number) entityManager.getEntityManager()
				.createNativeQuery("SELECT version FROM reviewable_course WHERE id = ?")
				.setParameter(1, reviewableCourseId.intValue())
				.getSingleResult();
		assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(0);
		assertThat(ReflectionTestUtils.getField(reloadedReviewer, "version")).isEqualTo(0);
		assertThat(reviewableCourseVersion.longValue()).isZero();
	}

	@Test
	void reviewer_updatedAgainstBigintColumn_linkedCourseReviewVersionUnchanged() throws Exception {
		// given - a course review wired to a freshly created reviewer FK parent on the migrated (BIGINT) schema,
		// both at version 0. The reviewer is created inline (rather than via newCourseReviewForMigratedSchema) so its
		// id is captured and can be updated on its own, and the reviewable_course parent is inserted with native SQL
		// for the same reason newCourseReviewForMigratedSchema documents (its entity maps original_course_id while
		// the migrated table exposes uuid).
		final Integer reviewerId = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("isolation-reviewer-" + UUID.randomUUID()))).getId();
		entityManager.getEntityManager()
				.createNativeQuery("INSERT INTO reviewable_course (uuid) VALUES (?)")
				.setParameter(1, UUID.randomUUID())
				.executeUpdate();
		final Number reviewableCourseId = (Number) entityManager.getEntityManager()
				.createNativeQuery("SELECT MAX(id) FROM reviewable_course")
				.getSingleResult();
		final Constructor<CourseReview> constructor = CourseReview.class
				.getDeclaredConstructor(ReviewCourseCommand.class, Integer.class, Integer.class);
		constructor.setAccessible(true);
		final CourseReview review = constructor.newInstance(
				new ReviewCourseCommand(UUID.randomUUID(), 4.0, "comment"), reviewableCourseId.intValue(), reviewerId);
		final UUID reviewUuid = review.toIdentifier();
		courseReviewRepository.saveAndFlush(review);
		entityManager.clear();

		// when - only the reviewer (the parent aggregate) is updated, bumping its BIGINT version to 1
		final Reviewer reloadedReviewer = reviewerRepository.findById(reviewerId).orElseThrow();
		ReflectionTestUtils.setField(reloadedReviewer, "username", "isolation-reviewer-renamed-" + UUID.randomUUID());
		reviewerRepository.saveAndFlush(reloadedReviewer);
		entityManager.clear();

		// then - the parent's version bump is isolated to the reviewer aggregate; the linked course_review is a
		// separate aggregate joined only by FK id, so its BIGINT version stays 0. This is the parent -> child
		// counterpart of courseReview_updatedAgainstBigintColumn_linkedParentVersionsUnchanged (child -> parents):
		// the Hibernate-schema slice asserts cross-aggregate isolation in both directions, but the migrated slice
		// only asserted child -> parents, leaving the reviewer -> course_review direction unverified on BIGINT.
		final Reviewer afterReload = reviewerRepository.findById(reviewerId).orElseThrow();
		final CourseReview linkedReview = courseReviewRepository.findByUuid(reviewUuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(afterReload, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(linkedReview, "version")).isEqualTo(0);
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

	@Test
	void reviewer_versionBumped_isReviewerQueryStillResolvesAcrossBigintJoin() throws Exception {
		// given - a course review on the migrated schema wired to a reviewer with a known username, both at version 0
		final String originalUsername = "isReviewer-reviewer-" + UUID.randomUUID();
		final Integer reviewerId = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand(originalUsername))).getId();
		final UUID reviewUuid = persistMigratedCourseReviewForReviewer(reviewerId);
		entityManager.clear();

		// baseline - the course_review -> reviewer join resolves the seeded reviewer on the migrated BIGINT schema
		assertThat(courseReviewRepository.isReviewer(reviewUuid, originalUsername)).isTrue();

		// when - the joined reviewer parent is renamed, bumping its BIGINT version 0 -> 1
		final String renamedUsername = "isReviewer-reviewer-renamed-" + UUID.randomUUID();
		final Reviewer reloadedReviewer = reviewerRepository.findById(reviewerId).orElseThrow();
		ReflectionTestUtils.setField(reloadedReviewer, "username", renamedUsername);
		reviewerRepository.saveAndFlush(reloadedReviewer);
		entityManager.clear();

		// then - the reviewer row really advanced past version 0, and the course_review -> reviewer join is
		// undisturbed by the BIGINT version bump: isReviewer resolves the now-current username and rejects the
		// stale one. The Hibernate slice proves this on the INTEGER column
		// (reviewer_versionBumped_listCourseReviewsAndIsReviewerStillResolveAcrossJoin), but the migrated slice never
		// exercised a read query, so the read path was unverified against the production-shaped BIGINT column.
		// isReviewer is the query that can run here: listCourseReviews projects reviewable_course.original_course_id,
		// which the migrated reviewable_course table does not expose.
		assertThat(ReflectionTestUtils.getField(
				reviewerRepository.findById(reviewerId).orElseThrow(), "version")).isEqualTo(1);
		assertThat(courseReviewRepository.isReviewer(reviewUuid, renamedUsername)).isTrue();
		assertThat(courseReviewRepository.isReviewer(reviewUuid, originalUsername)).isFalse();
	}

	@Test
	void courseReview_versionBumped_isReviewerQueryStillResolvesAgainstBigintColumn() throws Exception {
		// given - a course review on the migrated schema wired to a reviewer with a known username, both at version 0
		final String username = "isReviewer-reviewer-" + UUID.randomUUID();
		final Integer reviewerId = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand(username))).getId();
		final UUID reviewUuid = persistMigratedCourseReviewForReviewer(reviewerId);
		entityManager.clear();

		// when - the review itself is updated, bumping its own BIGINT version 0 -> 1
		final CourseReview reloaded = courseReviewRepository.findByUuid(reviewUuid).orElseThrow();
		reloaded.update(new UpdateCourseReviewCommand(reviewUuid, 5.0, "updated comment"));
		courseReviewRepository.saveAndFlush(reloaded);
		entityManager.clear();

		// then - the review row really advanced past version 0, and bumping the child course_review version leaves
		// the course_review -> reviewer join intact on the BIGINT column: isReviewer still resolves the reviewer and
		// rejects a non-reviewer. The child-side counterpart of the test above, mirroring the Hibernate slice's
		// courseReview_updatedBumpingVersion_isReviewerQueryStillResolves against the production-shaped column.
		assertThat(ReflectionTestUtils.getField(
				courseReviewRepository.findByUuid(reviewUuid).orElseThrow(), "version")).isEqualTo(1);
		assertThat(courseReviewRepository.isReviewer(reviewUuid, username)).isTrue();
		assertThat(courseReviewRepository.isReviewer(reviewUuid, "not-the-reviewer")).isFalse();
	}

	@Test
	void reviewer_concurrentUpdatesToDifferentRowsAgainstBigintColumn_bothSucceedAndEachVersionIncrements() {
		// given - two independently persisted reviewers on the migrated schema, both starting at version 0
		final Integer firstId = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("row-one-" + UUID.randomUUID()))).getId();
		final Integer secondId = reviewerRepository
				.saveAndFlush(new Reviewer(new CreateReviewerCommand("row-two-" + UUID.randomUUID()))).getId();
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

		// then - the optimistic-lock check is scoped per row, not per table, on the production-shaped BIGINT column:
		// committing one row does not stale a snapshot of a *different* row, so both writes succeed (neither throws)
		// and each BIGINT version advances independently to 1. The Hibernate slice proves this on the INTEGER column
		// (reviewer_concurrentUpdatesToDifferentRows_bothSucceedAndEachVersionIncrements); the migrated slice only
		// asserted the same-row stale-write rejection and single-row update isolation
		// (reviewer_updatingOneRowAgainstBigintColumn_otherRowsVersionUnchanged), never that two distinct pre-loaded
		// snapshots both commit, so a guard wrongly keyed off table-wide state would have slipped past it here.
		entityManager.clear();
		final Reviewer reloadedFirst = reviewerRepository.findById(firstId).orElseThrow();
		final Reviewer reloadedSecond = reviewerRepository.findById(secondId).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedFirst, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedSecond, "version")).isEqualTo(1);
	}

	@Test
	void courseReview_concurrentUpdatesToDifferentRowsAgainstBigintColumn_bothSucceedAndEachVersionIncrements()
			throws Exception {
		// given - two independently persisted reviews on the migrated schema, each wired to its own FK parents and
		// both starting at version 0
		final CourseReview firstRow = newCourseReviewForMigratedSchema(4.0, "first review");
		final UUID firstUuid = firstRow.toIdentifier();
		courseReviewRepository.saveAndFlush(firstRow);
		final CourseReview secondRow = newCourseReviewForMigratedSchema(3.0, "second review");
		final UUID secondUuid = secondRow.toIdentifier();
		courseReviewRepository.saveAndFlush(secondRow);
		entityManager.clear();

		// and - both rows are read into independent snapshots before either is written, modelling two concurrent
		// transactions that each loaded a *different* row while both rows were still at version 0
		final CourseReview firstSnapshot = courseReviewRepository.findByUuid(firstUuid).orElseThrow();
		entityManager.detach(firstSnapshot);
		final CourseReview secondSnapshot = courseReviewRepository.findByUuid(secondUuid).orElseThrow();
		entityManager.detach(secondSnapshot);

		// when - each snapshot writes its own row, the second one only after the first has already been committed
		firstSnapshot.update(new UpdateCourseReviewCommand(firstUuid, 5.0, "first row updated"));
		courseReviewRepository.saveAndFlush(firstSnapshot);
		secondSnapshot.update(new UpdateCourseReviewCommand(secondUuid, 2.0, "second row updated"));
		courseReviewRepository.saveAndFlush(secondSnapshot);

		// then - the optimistic-lock check is scoped per row, not per table, on the production-shaped BIGINT column:
		// committing one row does not stale a snapshot of a *different* row, so both writes succeed (neither throws)
		// and each BIGINT version advances independently to 1. Mirrors the Hibernate slice's
		// courseReview_concurrentUpdatesToDifferentRows_bothSucceedAndEachVersionIncrements onto the migrated column;
		// courseReview_updatingOneReviewAgainstBigintColumn_otherReviewsVersionUnchanged only updates one row and
		// asserts the other is untouched, never writing a pre-loaded snapshot of the second row after the first commit.
		entityManager.clear();
		final CourseReview reloadedFirst = courseReviewRepository.findByUuid(firstUuid).orElseThrow();
		final CourseReview reloadedSecond = courseReviewRepository.findByUuid(secondUuid).orElseThrow();
		assertThat(ReflectionTestUtils.getField(reloadedFirst, "version")).isEqualTo(1);
		assertThat(ReflectionTestUtils.getField(reloadedSecond, "version")).isEqualTo(1);
	}

	/**
	 * Persists a {@link CourseReview} bound to the given reviewer (and a freshly created reviewable-course FK parent)
	 * on the migrated schema, returning its uuid. Used by the {@code isReviewer} read-query tests, which need the
	 * reviewer's id captured so its row can be bumped independently of {@link #newCourseReviewForMigratedSchema}.
	 */
	private UUID persistMigratedCourseReviewForReviewer(Integer reviewerId) throws Exception {
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
		final CourseReview review = constructor.newInstance(
				new ReviewCourseCommand(UUID.randomUUID(), 4.0, "comment"), courseId.intValue(), reviewerId);
		final UUID reviewUuid = review.toIdentifier();
		courseReviewRepository.saveAndFlush(review);
		return reviewUuid;
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
