package com.educational.platform.course.reviews.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import liquibase.integration.spring.SpringLiquibase;

/**
 * Verifies the Liquibase changeSets in {@code db/course-reviews.yml} that back the new {@code @Version}
 * fields: each of the three tables must gain a non-null {@code version} column defaulting to {@code 0}.
 *
 * <p>The {@link OptimisticLockingTest} slice runs against a Hibernate-generated schema (no Liquibase on
 * its classpath), so it never exercises the migration that ships to production. This test runs the real
 * changelog against H2 and inspects the resulting schema, so a broken {@code add-version-column-to-*}
 * changeSet is caught here rather than only at deploy time.
 */
class CourseReviewsLiquibaseMigrationTest {

	private DriverManagerDataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void applyCourseReviewsChangelog() throws Exception {
		// URL-only datasource: the H2 driver auto-registers via the JDBC SPI, so the test stays
		// independent of the (runtime-only) H2 dependency just like the @DataJpaTest slices.
		dataSource = new DriverManagerDataSource(
				"jdbc:h2:mem:course-reviews-migration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
		jdbcTemplate = new JdbcTemplate(dataSource);

		final SpringLiquibase liquibase = new SpringLiquibase();
		liquibase.setDataSource(dataSource);
		liquibase.setChangeLog("classpath:db/course-reviews.yml");
		liquibase.setResourceLoader(new DefaultResourceLoader());
		liquibase.afterPropertiesSet();
	}

	@AfterEach
	void shutdown() {
		jdbcTemplate.execute("SHUTDOWN");
	}

	@ParameterizedTest
	@ValueSource(strings = {"COURSE_REVIEW", "REVIEWABLE_COURSE", "REVIEWER"})
	void migration_addsNonNullBigintVersionColumnDefaultingToZero(String table) throws Exception {
		// when - the column metadata produced by the changelog is read back
		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, table, "VERSION")) {

			// then - a single version column exists with the type/nullability/default declared in the changeSet
			assertThat(columns.next()).as("version column should exist on %s", table).isTrue();
			assertThat(columns.getString("TYPE_NAME")).as("version column type on %s", table).isEqualTo("BIGINT");
			assertThat(columns.getInt("NULLABLE")).as("version column should be NOT NULL on %s", table)
					.isEqualTo(DatabaseMetaData.columnNoNulls);
			assertThat(columns.getString("COLUMN_DEF")).as("version column default on %s", table).contains("0");
			assertThat(columns.next()).as("only one version column should exist on %s", table).isFalse();
		}
	}

	@Test
	void migration_reviewerInsertedWithoutVersion_versionDefaultsToZero() {
		// given/when - a row is inserted without specifying the new column
		jdbcTemplate.update("INSERT INTO reviewer (username) VALUES ('migration-default-reviewer')");

		// then - the changeSet default backfills version to 0 (the value real rows get in production,
		// mirrored by the test seed SQL so updates do not increment a null version)
		final Long version = jdbcTemplate.queryForObject(
				"SELECT version FROM reviewer WHERE username = 'migration-default-reviewer'", Long.class);
		assertThat(version).isZero();
	}

	@Test
	void migration_reviewableCourseInsertedWithoutVersion_versionDefaultsToZero() {
		// given/when - a row is inserted without specifying the new column
		jdbcTemplate.update("INSERT INTO reviewable_course (uuid) VALUES (?)", UUID.randomUUID());

		// then - the changeSet default backfills version to 0
		final Long version = jdbcTemplate.queryForObject("SELECT MAX(version) FROM reviewable_course", Long.class);
		assertThat(version).isZero();
	}

	@Test
	void migration_courseReviewInsertedWithoutVersion_versionDefaultsToZero() {
		// given - the foreign-key parents required by the course_review NOT NULL/FK constraints
		jdbcTemplate.update("INSERT INTO reviewer (username) VALUES ('migration-default-cr-reviewer')");
		jdbcTemplate.update("INSERT INTO reviewable_course (uuid) VALUES (?)", UUID.randomUUID());

		// when - a course_review row is inserted without specifying the new column
		jdbcTemplate.update("INSERT INTO course_review (uuid, reviewer, course, rating, comment) VALUES (?, "
				+ "(SELECT id FROM reviewer WHERE username = 'migration-default-cr-reviewer'), "
				+ "(SELECT MAX(id) FROM reviewable_course), 4.0, 'comment')", UUID.randomUUID());

		// then - the changeSet default backfills version to 0 (the value real rows get in production,
		// mirrored by the test seed SQL so updating a row does not increment a null version)
		final Long version = jdbcTemplate.queryForObject("SELECT version FROM course_review", Long.class);
		assertThat(version).isZero();
	}

	@Test
	void migration_appliedTwice_isIdempotent() throws Exception {
		// given - the changelog already applied once by @BeforeEach
		// when - the same changelog is applied a second time against the same datasource
		final SpringLiquibase rerun = new SpringLiquibase();
		rerun.setDataSource(dataSource);
		rerun.setChangeLog("classpath:db/course-reviews.yml");
		rerun.setResourceLoader(new DefaultResourceLoader());
		rerun.afterPropertiesSet();

		// then - Liquibase skips the already-applied add-version-column changeSets instead of failing,
		// and the version column is not duplicated on the table (re-running migrations is a deploy-time
		// reality, so the changeSets must stay idempotent)
		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "REVIEWER", "VERSION")) {
			assertThat(columns.next()).as("version column should still exist after re-running the changelog").isTrue();
			assertThat(columns.next()).as("re-running the changelog should not duplicate the version column").isFalse();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"COURSE_REVIEW", "REVIEWABLE_COURSE"})
	void migration_appliedTwice_versionColumnNotDuplicated(String table) throws Exception {
		// given - the changelog already applied once by @BeforeEach
		// when - the same changelog is applied a second time against the same datasource
		final SpringLiquibase rerun = new SpringLiquibase();
		rerun.setDataSource(dataSource);
		rerun.setChangeLog("classpath:db/course-reviews.yml");
		rerun.setResourceLoader(new DefaultResourceLoader());
		rerun.afterPropertiesSet();

		// then - the add-version-column changeSet for each remaining table is idempotent too: re-running
		// does not duplicate the version column (migration_appliedTwice_isIdempotent only covers reviewer,
		// leaving the course_review and reviewable_course changeSets unverified against a re-run)
		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, table, "VERSION")) {
			assertThat(columns.next()).as("version column should still exist on %s after re-run", table).isTrue();
			assertThat(columns.next()).as("re-running the changelog should not duplicate the version column on %s", table)
					.isFalse();
		}
	}
}
