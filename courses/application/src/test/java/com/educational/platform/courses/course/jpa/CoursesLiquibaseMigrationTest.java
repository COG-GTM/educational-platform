package com.educational.platform.courses.course.jpa;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Verifies the three Liquibase change sets this PR adds to {@code db/courses.yml}, which introduce
 * the optimistic-lock {@code version} column on {@code course}, {@code curriculum_item} and
 * {@code teacher}.
 *
 * <p>The {@code @DataJpaTest} suites in this package run against the Hibernate-generated schema, so
 * they exercise the {@code @Version} <em>mapping</em> but never the production DDL. Here we apply the
 * real changelog with Liquibase against an in-memory H2 database (matching how the production schema
 * is built, {@code ddl-auto=none}) and assert the column contract the entities depend on: a non-null
 * {@code BIGINT} defaulting to {@code 0}.
 */
class CoursesLiquibaseMigrationTest {

    private static final String CHANGELOG = "db/courses.yml";

    private Connection connection;

    @BeforeEach
    void applyCoursesChangelog() throws Exception {
        // DB_CLOSE_DELAY=-1 keeps the in-memory database alive for the assertion connection even after
        // Liquibase closes its own connection on close()
        final String url = "jdbc:h2:mem:courses-migration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        connection = DriverManager.getConnection(url, "sa", "");

        try (Connection migrationConnection = DriverManager.getConnection(url, "sa", "")) {
            final Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(migrationConnection));
            try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    @AfterEach
    void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void coursesChangelog_addsVersionColumnToCourse_asNonNullBigint() throws SQLException {
        assertVersionColumnIsNonNullBigint("COURSE");
    }

    @Test
    void coursesChangelog_addsVersionColumnToCurriculumItem_asNonNullBigint() throws SQLException {
        assertVersionColumnIsNonNullBigint("CURRICULUM_ITEM");
    }

    @Test
    void coursesChangelog_addsVersionColumnToTeacher_asNonNullBigint() throws SQLException {
        assertVersionColumnIsNonNullBigint("TEACHER");
    }

    @Test
    void teacherVersionColumn_defaultsToZeroWhenOmittedOnInsert() throws SQLException {
        // the @Version Integer field is unset on a brand-new aggregate that bypasses Hibernate's
        // version initialisation (e.g. the SQL-seeded fixtures); the DEFAULT 0 keeps it non-null
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO teacher (username) VALUES ('teacher')");
        }

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT version FROM teacher WHERE username = 'teacher'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong("version")).isZero();
        }
    }

    @Test
    void teacherVersionColumn_rejectsExplicitNull() {
        // the non-null constraint is what stopped Hibernate NPE-ing on increment(null) during flush
        assertThatExceptionOfType(SQLException.class).isThrownBy(() -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO teacher (username, version) VALUES ('teacher', NULL)");
            }
        });
    }

    private void assertVersionColumnIsNonNullBigint(String table) throws SQLException {
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
        }
    }

    private static long numericDefault(String columnDefault) {
        assertThat(columnDefault).as("column default expression").isNotNull();
        return Long.parseLong(columnDefault.replaceAll("[^0-9-]", ""));
    }
}
