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
