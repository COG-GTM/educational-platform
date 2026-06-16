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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the three {@code addColumn} change sets this PR adds to {@code db/courses.yml} are safe to
 * apply to <em>already-populated</em> tables.
 *
 * <p>{@link CoursesLiquibaseMigrationTest} proves the column contract (non-null {@code BIGINT}
 * defaulting to {@code 0}) and that the {@code DEFAULT 0} kicks in on <em>new</em> inserts. It never
 * exercises the riskier path the {@code defaultValueNumeric: 0} + {@code nullable: false} combination
 * actually exists for: adding a {@code NOT NULL} column to a table that already holds rows. Without
 * the default, that {@code ALTER TABLE} would fail (or leave the existing rows {@code NULL},
 * violating the new constraint). Here we apply the base schema, seed rows <em>before</em> the
 * {@code version} column exists, then apply the add-version change sets and assert the pre-existing
 * rows are back-filled to {@code 0} - the migration-safety guarantee on a live table.
 */
class CoursesVersionColumnBackfillMigrationTest {

    private static final String CHANGELOG = "db/courses.yml";

    // db/courses.yml declares 7 base change sets (the create-table + add-foreign-key sets,
    // ids 2021_06_25-1..7) ahead of the three add-version change sets this PR appends last. We apply
    // exactly those base sets, seed rows, then let update() run the remaining version change sets.
    private static final int BASE_CHANGE_SETS_BEFORE_VERSION = 7;

    private String url;
    private Connection migrationConnection;
    private Liquibase liquibase;
    private Connection connection;

    @BeforeEach
    void applyBaseSchemaWithoutVersionColumn() throws Exception {
        // DB_CLOSE_DELAY=-1 keeps the in-memory database alive across the separate migration and
        // assertion connections
        url = "jdbc:h2:mem:courses-backfill-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        connection = DriverManager.getConnection(url, "sa", "");
        migrationConnection = DriverManager.getConnection(url, "sa", "");

        final Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(migrationConnection));
        liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);

        // apply only the base schema; the version column does not exist yet
        liquibase.update(BASE_CHANGE_SETS_BEFORE_VERSION, new Contexts(), new LabelExpression());
    }

    @AfterEach
    void close() throws Exception {
        if (liquibase != null) {
            liquibase.close();
        }
        if (migrationConnection != null) {
            migrationConnection.close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void addVersionColumn_backfillsExistingTeacherRowToZero() throws Exception {
        final int teacherId = insertTeacher();

        applyVersionChangeSets();

        assertThat(versionOf("teacher", teacherId)).isZero();
    }

    @Test
    void addVersionColumn_backfillsExistingCourseRowToZero() throws Exception {
        final int teacherId = insertTeacher();
        final int courseId = insertCourse(teacherId);

        applyVersionChangeSets();

        assertThat(versionOf("course", courseId)).isZero();
    }

    @Test
    void addVersionColumn_backfillsExistingCurriculumItemRowToZero() throws Exception {
        final int teacherId = insertTeacher();
        final int courseId = insertCourse(teacherId);
        final int itemId = insertCurriculumItem(courseId);

        applyVersionChangeSets();

        assertThat(versionOf("curriculum_item", itemId)).isZero();
    }

    private void applyVersionChangeSets() throws Exception {
        // runs the remaining (pending) change sets - the three add-version sets - against the
        // already-populated tables
        liquibase.update(new Contexts(), new LabelExpression());
    }

    private int insertTeacher() throws SQLException {
        return insertReturningId("INSERT INTO teacher (username) VALUES ('teacher')");
    }

    private int insertCourse(int teacherId) throws SQLException {
        return insertReturningId("INSERT INTO course "
                + "(uuid, name, description, publish_status, approval_status, rating, number_of_students, teacher) "
                + "VALUES (RANDOM_UUID(), 'name', 'description', 'DRAFT', 'APPROVED', 0, 0, " + teacherId + ")");
    }

    private int insertCurriculumItem(int courseId) throws SQLException {
        return insertReturningId("INSERT INTO curriculum_item "
                + "(uuid, title, description, serial_number, course, type, content) "
                + "VALUES (RANDOM_UUID(), 'title', 'description', '1', " + courseId + ", 'LECTURE', 'content')");
    }

    private int insertReturningId(String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                return keys.getInt(1);
            }
        }
    }

    private long versionOf(String table, int id) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT version FROM " + table + " WHERE id = " + id)) {
            assertThat(rs.next()).isTrue();
            final long version = rs.getLong("version");
            // getLong() maps SQL NULL to 0, which would hide a regression that dropped the
            // defaultValueNumeric and left existing rows NULL; assert the back-fill is a real value
            assertThat(rs.wasNull()).as("back-filled version on %s should be non-null", table).isFalse();
            return version;
        }
    }
}
