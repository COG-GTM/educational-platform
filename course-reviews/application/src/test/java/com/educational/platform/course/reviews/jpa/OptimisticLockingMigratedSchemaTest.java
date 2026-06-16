package com.educational.platform.course.reviews.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
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
