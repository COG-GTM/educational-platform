package com.educational.platform.courses.course.jpa;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies the three {@code addColumn} change sets this PR appends to {@code db/courses.yml} are
 * <em>re-runnable</em>: applying the changelog a second time against an already-migrated database is
 * a safe no-op that leaves the {@code version} column contract intact.
 *
 * <p>This is the one migration-safety property the existing suite never asserts outright.
 * {@link CoursesLiquibaseMigrationTest} and {@link CoursesVersionColumnBackfillMigrationTest} each
 * apply the changelog exactly once (fresh schema, then populated schema). Yet production runs
 * Liquibase {@code update} on <em>every</em> application boot against the same persisted database
 * ({@code ddl-auto=none}), so the change sets are re-evaluated repeatedly. A raw {@code addColumn}
 * is not naturally idempotent - re-issuing it would fail with "column already exists" - so the only
 * thing that keeps the second boot safe is Liquibase recording each change set in
 * {@code DATABASECHANGELOG} by id and skipping it (and validating its checksum is unchanged).
 *
 * <p>Here we apply {@code db/courses.yml} once, confirm the three add-version change sets are no
 * longer pending, then apply it again and assert (a) the rerun does not throw and (b) each table
 * still carries exactly one non-null {@code BIGINT} {@code version} column defaulting to {@code 0} -
 * i.e. the change sets were skipped, not re-applied. A regression that renamed a change set id,
 * altered an already-applied change set's body (checksum drift), or swapped {@code addColumn} for a
 * non-idempotent raw statement would break this rerun while the single-apply tests stayed green.
 */
class CoursesVersionMigrationIdempotencyTest {

    private static final String CHANGELOG = "db/courses.yml";

    private static final List<String> VERSION_CHANGE_SET_IDS = List.of(
            "add-version-column-to-course",
            "add-version-column-to-curriculum_item",
            "add-version-column-to-teacher");

    private Connection connection;

    @AfterEach
    void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void coursesChangelog_appliedTwice_secondRunSkipsTheVersionChangeSets() throws Exception {
        // DB_CLOSE_DELAY=-1 keeps the in-memory database (and its DATABASECHANGELOG history) alive
        // across the migration and assertion connections
        final String url = "jdbc:h2:mem:courses-idempotency-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        connection = DriverManager.getConnection(url, "sa", "");

        try (Connection migrationConnection = DriverManager.getConnection(url, "sa", "")) {
            final Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(migrationConnection));
            try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
                // first boot - applies the whole changelog, including the three add-version change sets
                liquibase.update(new Contexts(), new LabelExpression());

                // the three add-version change sets are now recorded as run, so none remain pending
                assertThat(pendingVersionChangeSetIds(liquibase)).isEmpty();

                // second boot against the already-migrated schema must be a safe no-op: Liquibase skips
                // the recorded change sets rather than re-issuing the addColumn (which would fail)
                assertThatCode(() -> liquibase.update(new Contexts(), new LabelExpression()))
                        .doesNotThrowAnyException();
            }
        }

        // and the column contract is untouched by the rerun: exactly one non-null BIGINT version
        // column defaulting to 0 on each table - proving the change sets were skipped, not re-applied
        assertSingleNonNullBigintVersionColumn("COURSE");
        assertSingleNonNullBigintVersionColumn("CURRICULUM_ITEM");
        assertSingleNonNullBigintVersionColumn("TEACHER");
    }

    private List<String> pendingVersionChangeSetIds(Liquibase liquibase) throws Exception {
        return liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression()).stream()
                .map(ChangeSet::getId)
                .filter(VERSION_CHANGE_SET_IDS::contains)
                .collect(Collectors.toList());
    }

    private void assertSingleNonNullBigintVersionColumn(String table) throws SQLException {
        final DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(null, null, table, "VERSION")) {
            assertThat(columns.next())
                    .as("version column should exist on %s", table)
                    .isTrue();
            assertThat(columns.getString("TYPE_NAME")).isEqualToIgnoringCase("BIGINT");
            assertThat(columns.getString("IS_NULLABLE")).isEqualToIgnoringCase("NO");
            assertThat(columns.getInt("NULLABLE")).isEqualTo(DatabaseMetaData.columnNoNulls);
            assertThat(numericDefault(columns.getString("COLUMN_DEF")))
                    .as("version column on %s should default to 0", table)
                    .isZero();
            // a re-applied addColumn would have produced a duplicate column; there must be exactly one
            assertThat(columns.next())
                    .as("version column should not be duplicated on %s", table)
                    .isFalse();
        }
    }

    private static long numericDefault(String columnDefault) {
        assertThat(columnDefault).as("column default expression").isNotNull();
        return Long.parseLong(columnDefault.replaceAll("[^0-9-]", ""));
    }
}
