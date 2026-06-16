package com.educational.platform.course.reviews.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.resource.ClassLoaderResourceAccessor;

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
	void migration_reviewerInsertedWithExplicitNullVersion_isRejected() {
		// when/then - inserting a row that explicitly sets version to NULL is rejected. The metadata test
		// only proves the column is *declared* NOT NULL; this proves the constraint is actually enforced at
		// runtime (the default fills an omitted column, but it does not rescue an explicit NULL), so a row
		// can never persist without the comparable version Hibernate's @Version guard relies on.
		assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
				jdbcTemplate.update("INSERT INTO reviewer (username, version) VALUES ('null-version-reviewer', NULL)"));
	}

	@Test
	void migration_reviewableCourseInsertedWithExplicitNullVersion_isRejected() {
		// when/then - the nullable:false constraint is enforced for the reviewable_course version column too
		assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
				jdbcTemplate.update("INSERT INTO reviewable_course (uuid, version) VALUES (?, NULL)", UUID.randomUUID()));
	}

	@Test
	void migration_courseReviewInsertedWithExplicitNullVersion_isRejected() {
		// given - the foreign-key parents required by the course_review NOT NULL/FK constraints
		jdbcTemplate.update("INSERT INTO reviewer (username) VALUES ('null-version-cr-reviewer')");
		jdbcTemplate.update("INSERT INTO reviewable_course (uuid) VALUES (?)", UUID.randomUUID());

		// when/then - inserting a course_review with an explicit NULL version is rejected, so the headline
		// aggregate can never be persisted in a versionless state on the production-shaped schema
		assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
				jdbcTemplate.update("INSERT INTO course_review (uuid, reviewer, course, rating, comment, version) VALUES (?, "
						+ "(SELECT id FROM reviewer WHERE username = 'null-version-cr-reviewer'), "
						+ "(SELECT MAX(id) FROM reviewable_course), 4.0, 'comment', NULL)", UUID.randomUUID()));
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

	@Test
	void migration_versionColumnAddedToTablesWithExistingRows_backfillsExistingRowsToZeroAndPreservesData() throws Exception {
		// given - the base schema only (the five 2021_06_25-* changeSets), built before any version column
		// exists. This reproduces the production upgrade path: the three tables already hold rows when the
		// add-version-column-* changeSets run. Every other test in this class inserts *after* the column
		// already exists, so the backfill of pre-existing rows - the actual deploy-time behaviour - was never
		// exercised. If Liquibase added the NOT NULL column before populating the default, the migration would
		// fail on any non-empty production table; this proves it does not.
		try (PartialMigration migration = applyBaseSchemaOnly()) {
			final JdbcTemplate jdbc = migration.jdbcTemplate();
			assertVersionColumnAbsent(migration.dataSource(), "REVIEWER");

			jdbc.update("INSERT INTO reviewer (username) VALUES ('pre-existing-reviewer')");
			jdbc.update("INSERT INTO reviewable_course (uuid) VALUES (?)", UUID.randomUUID());
			jdbc.update("INSERT INTO course_review (uuid, reviewer, course, rating, comment) VALUES (?, "
					+ "(SELECT id FROM reviewer WHERE username = 'pre-existing-reviewer'), "
					+ "(SELECT MAX(id) FROM reviewable_course), 4.0, 'great course')", UUID.randomUUID());

			// when - the add-version-column-* changeSets are applied to the already-populated tables
			migration.liquibase().update(new Contexts(), new LabelExpression());

			// then - the NOT NULL version column is backfilled to its default (0) for every pre-existing row,
			// so the migration neither fails on populated tables nor leaves a row with a null version that
			// Hibernate's @Version guard could not compare against
			assertThat(jdbc.queryForObject(
					"SELECT version FROM reviewer WHERE username = 'pre-existing-reviewer'", Long.class)).isZero();
			assertThat(jdbc.queryForObject("SELECT version FROM reviewable_course", Long.class)).isZero();
			assertThat(jdbc.queryForObject("SELECT version FROM course_review", Long.class)).isZero();

			// and - the existing business data survives the migration untouched
			assertThat(jdbc.queryForObject("SELECT rating FROM course_review", Double.class)).isEqualTo(4.0);
			assertThat(jdbc.queryForObject("SELECT comment FROM course_review", String.class)).isEqualTo("great course");
		}
	}

	@Test
	void migration_versionColumnAddedToTableWithMultipleExistingRows_backfillsEveryRowToZero() throws Exception {
		// given - several rows already in reviewer before the version column exists
		try (PartialMigration migration = applyBaseSchemaOnly()) {
			final JdbcTemplate jdbc = migration.jdbcTemplate();
			jdbc.update("INSERT INTO reviewer (username) VALUES ('reviewer-a')");
			jdbc.update("INSERT INTO reviewer (username) VALUES ('reviewer-b')");
			jdbc.update("INSERT INTO reviewer (username) VALUES ('reviewer-c')");

			// when - the version column is added to the populated table
			migration.liquibase().update(new Contexts(), new LabelExpression());

			// then - every pre-existing row is backfilled to 0 (not just one), so the default applies to all
			// historical data rather than to a single arbitrary row
			assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reviewer WHERE version = 0", Long.class)).isEqualTo(3L);
			assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reviewer WHERE version IS NULL", Long.class)).isZero();
		}
	}

	@Test
	void migration_versionColumnAddedToCourseReviewWithMultipleExistingRows_backfillsEveryRowToZero() throws Exception {
		// given - several course_review rows already present before the version column exists. This is the
		// headline FK-constrained aggregate, not the trivial reviewer table: the backfill-every-row case was
		// only proven for reviewer, so the production upgrade of an already-populated course_review table -
		// the table whose updates @Version is actually meant to guard - was left unverified for multiple rows.
		try (PartialMigration migration = applyBaseSchemaOnly()) {
			final JdbcTemplate jdbc = migration.jdbcTemplate();
			jdbc.update("INSERT INTO reviewer (username) VALUES ('multi-row-reviewer')");
			jdbc.update("INSERT INTO reviewable_course (uuid) VALUES (?)", UUID.randomUUID());
			for (int rating = 1; rating <= 3; rating++) {
				jdbc.update("INSERT INTO course_review (uuid, reviewer, course, rating, comment) VALUES (?, "
						+ "(SELECT id FROM reviewer WHERE username = 'multi-row-reviewer'), "
						+ "(SELECT MAX(id) FROM reviewable_course), ?, 'comment')", UUID.randomUUID(), (double) rating);
			}

			// when - the version column is added to the populated, FK-constrained table
			migration.liquibase().update(new Contexts(), new LabelExpression());

			// then - every pre-existing review is backfilled to 0 (not just one), and the ratings survive,
			// so the migration neither fails on a non-empty course_review table nor leaves any row with a
			// null version Hibernate's @Version guard could not compare against
			assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM course_review WHERE version = 0", Long.class)).isEqualTo(3L);
			assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM course_review WHERE version IS NULL", Long.class)).isZero();
			assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM course_review", Long.class)).isEqualTo(3L);
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

	// The five 2021_06_25-* changeSets that build the base schema, before the three add-version-column-*
	// changeSets. Applying exactly these reproduces a production database as it looked before this PR; the
	// assertVersionColumnAbsent guard fails loudly if a base changeSet is ever added and this count drifts.
	private static final int BASE_SCHEMA_CHANGE_SET_COUNT = 5;

	/**
	 * Builds a fresh in-memory database with only the base course-reviews schema applied (no version columns
	 * yet), leaving the returned {@link Liquibase} positioned to apply the remaining add-version-column-*
	 * changeSets once test data has been inserted.
	 */
	private PartialMigration applyBaseSchemaOnly() throws Exception {
		final DriverManagerDataSource ds = new DriverManagerDataSource(
				"jdbc:h2:mem:course-reviews-existing-rows-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
		final Database database = DatabaseFactory.getInstance()
				.findCorrectDatabaseImplementation(new JdbcConnection(ds.getConnection()));
		final Liquibase liquibase = new Liquibase("db/course-reviews.yml", new ClassLoaderResourceAccessor(), database);
		liquibase.update(BASE_SCHEMA_CHANGE_SET_COUNT, new Contexts(), new LabelExpression());
		return new PartialMigration(ds, new JdbcTemplate(ds), liquibase);
	}

	private static void assertVersionColumnAbsent(DriverManagerDataSource dataSource, String table) throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, table, "VERSION")) {
			assertThat(columns.next())
					.as("base schema should not yet have a version column on %s (guards the changeSet count)", table)
					.isFalse();
		}
	}

	private record PartialMigration(DriverManagerDataSource dataSource, JdbcTemplate jdbcTemplate, Liquibase liquibase)
			implements AutoCloseable {
		@Override
		public void close() {
			try {
				liquibase.close();
			} catch (Exception ignored) {
				// best-effort cleanup of the Liquibase-held connection before the in-memory database is dropped
			}
			jdbcTemplate.execute("SHUTDOWN");
		}
	}
}
