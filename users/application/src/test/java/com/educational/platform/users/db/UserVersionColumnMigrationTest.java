package com.educational.platform.users.db;

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
import java.sql.Types;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Verifies the Liquibase changeSet {@code add-version-column-to-custom_user} in {@code db/users.yml}
 * that backs the {@code @Version} optimistic-locking field on
 * {@link com.educational.platform.users.User}.
 *
 * <p>The changelog is executed against an in-memory H2 database. The {@code custom_user} table is created
 * up front so the original {@code createTable} changeSet is skipped via its {@code not tableExists}
 * precondition ({@code MARK_RAN}) - leaving only the new column migration to run, exactly the path an
 * existing deployment takes when this upgrade is applied. A single keep-alive connection holds the
 * in-memory database open for the duration of each test while individual operations use their own
 * short-lived connections (Liquibase closes the connection it is handed).
 */
class UserVersionColumnMigrationTest {

    private static final String CHANGELOG = "db/users.yml";

    private String jdbcUrl;
    private Connection keepAlive;

    @BeforeEach
    void setUp() throws SQLException {
        jdbcUrl = "jdbc:h2:mem:users-migration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        keepAlive = openConnection();
    }

    @AfterEach
    void tearDown() throws SQLException {
        keepAlive.close();
    }

    @Test
    void migration_addsNotNullBigIntVersionColumn() throws Exception {
        givenLegacyCustomUserTable();

        runUsersChangelog();

        try (Connection connection = openConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, "CUSTOM_USER", "VERSION")) {
            assertThat(columns.next()).as("version column is present on custom_user").isTrue();
            assertThat(columns.getInt("DATA_TYPE")).isEqualTo(Types.BIGINT);
            assertThat(columns.getString("IS_NULLABLE")).isEqualTo("NO");
        }
    }

    @Test
    void migration_freshInstall_createsTableWithVersionColumn() throws Exception {
        // given no pre-existing custom_user table: the createTable precondition
        // (not tableExists) is satisfied, so the full changelog runs end to end.
        runUsersChangelog();

        try (Connection connection = openConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, "CUSTOM_USER", "VERSION")) {
            assertThat(columns.next()).as("version column is present on a freshly created custom_user").isTrue();
            assertThat(columns.getInt("DATA_TYPE")).isEqualTo(Types.BIGINT);
            assertThat(columns.getString("IS_NULLABLE")).isEqualTo("NO");
        }
    }

    @Test
    void migration_backfillsExistingRowsToZero() throws Exception {
        givenLegacyCustomUserTable();
        insertUserWithoutVersion("legacy");

        runUsersChangelog();

        assertThat(versionOf("legacy")).isZero();
    }

    @Test
    void migration_backfillsMultipleExistingRowsToZero() throws Exception {
        givenLegacyCustomUserTable();
        insertUserWithoutVersion("legacy-1");
        insertUserWithoutVersion("legacy-2");
        insertUserWithoutVersion("legacy-3");

        runUsersChangelog();

        assertThat(versionOf("legacy-1")).isZero();
        assertThat(versionOf("legacy-2")).isZero();
        assertThat(versionOf("legacy-3")).isZero();
    }

    @Test
    void migration_runningTwice_isIdempotent() throws Exception {
        givenLegacyCustomUserTable();
        insertUserWithoutVersion("legacy");

        // running the changelog twice must not fail or re-apply the changeSet
        runUsersChangelog();
        runUsersChangelog();

        assertThat(versionOf("legacy")).isZero();
        try (Connection connection = openConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, "CUSTOM_USER", "VERSION")) {
            assertThat(columns.next()).as("version column is present").isTrue();
            assertThat(columns.getInt("DATA_TYPE")).isEqualTo(Types.BIGINT);
            assertThat(columns.getString("IS_NULLABLE")).isEqualTo("NO");
            assertThat(columns.next()).as("exactly one version column exists after a second run").isFalse();
        }
    }

    @Test
    void migration_newRowWithoutVersion_defaultsToZero() throws Exception {
        givenLegacyCustomUserTable();
        runUsersChangelog();

        insertUserWithoutVersion("fresh");

        assertThat(versionOf("fresh")).isZero();
    }

    @Test
    void migration_freshInstall_newRowWithoutVersion_defaultsToZero() throws Exception {
        // given no pre-existing table: createTable + addColumn both run, exercising the
        // full changelog (not just the addColumn upgrade path)
        runUsersChangelog();

        insertUserWithoutVersion("fresh");

        // then the column DEFAULT 0 still applies to inserts that omit the version
        assertThat(versionOf("fresh")).isZero();
    }

    @Test
    void migration_preservesExistingRowDataWhileBackfillingVersion() throws Exception {
        givenLegacyCustomUserTable();
        insertUserWithoutVersion("legacy");

        runUsersChangelog();

        // the additive migration backfills the version without disturbing the other columns of an existing row
        assertThat(versionOf("legacy")).isZero();
        assertThat(stringColumnOf("legacy", "email")).isEqualTo("legacy@gmail.com");
        assertThat(stringColumnOf("legacy", "password")).isEqualTo("password");
        assertThat(stringColumnOf("legacy", "role")).isEqualTo("ROLE_STUDENT");
    }

    @Test
    void migration_reRun_doesNotResetAdvancedVersionValues() throws Exception {
        givenLegacyCustomUserTable();
        insertUserWithoutVersion("legacy");
        runUsersChangelog();

        // a live row whose version has since advanced past the default
        setVersion("legacy", 5L);

        // re-running the changelog must not re-apply the changeSet and clobber the live version
        runUsersChangelog();

        assertThat(versionOf("legacy")).isEqualTo(5L);
    }

    @Test
    void migration_insertingNullVersion_violatesNotNullConstraint() throws Exception {
        givenLegacyCustomUserTable();
        runUsersChangelog();

        assertThatExceptionOfType(SQLException.class).isThrownBy(() -> {
            try (Connection connection = openConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO custom_user (username, email, password, role, version) VALUES (?, ?, ?, ?, ?)")) {
                statement.setString(1, "broken");
                statement.setString(2, "broken@gmail.com");
                statement.setString(3, "password");
                statement.setString(4, "ROLE_STUDENT");
                statement.setNull(5, Types.BIGINT);
                statement.executeUpdate();
            }
        });
    }

    @Test
    void migration_recordsVersionChangeSetExactlyOnce() throws Exception {
        givenLegacyCustomUserTable();

        runUsersChangelog();
        // a second run must not re-record the changeSet
        runUsersChangelog();

        // the changeSet identity (id/author) is what makes the migration idempotent; assert it ran once
        assertThat(changeSetExecutionCount("add-version-column-to-custom_user", "devin")).isEqualTo(1);
    }

    @Test
    void migration_rollback_removesVersionColumnButPreservesTableAndData() throws Exception {
        givenLegacyCustomUserTable();
        insertUserWithoutVersion("legacy");
        runUsersChangelog();

        // when the version changeSet is rolled back (downgrade path - addColumn is auto-rollbackable)
        rollbackUsersChangelog(1);

        // then only the additive column is reverted: the column and its changelog record disappear
        assertThat(versionColumnExists()).as("version column is dropped on rollback").isFalse();
        assertThat(changeSetExecutionCount("add-version-column-to-custom_user", "devin")).isZero();
        // while the pre-existing table and its row data are left untouched
        assertThat(stringColumnOf("legacy", "email")).isEqualTo("legacy@gmail.com");
        assertThat(stringColumnOf("legacy", "role")).isEqualTo("ROLE_STUDENT");
    }

    @Test
    void migration_rollbackThenReRun_reAddsVersionColumnAndBackfillsToZero() throws Exception {
        givenLegacyCustomUserTable();
        insertUserWithoutVersion("legacy");
        runUsersChangelog();
        rollbackUsersChangelog(1);

        // when the changelog is re-applied after a rollback (a downgrade followed by a re-upgrade)
        runUsersChangelog();

        // then the migration is cleanly repeatable: the column is restored, recorded once, and backfilled
        try (Connection connection = openConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, "CUSTOM_USER", "VERSION")) {
            assertThat(columns.next()).as("version column is re-added after rollback").isTrue();
            assertThat(columns.getInt("DATA_TYPE")).isEqualTo(Types.BIGINT);
            assertThat(columns.getString("IS_NULLABLE")).isEqualTo("NO");
        }
        assertThat(changeSetExecutionCount("add-version-column-to-custom_user", "devin")).isEqualTo(1);
        assertThat(versionOf("legacy")).isZero();
    }

    @Test
    void migration_versionColumn_storesValuesBeyondIntegerRange() throws Exception {
        givenLegacyCustomUserTable();
        insertUserWithoutVersion("legacy");
        runUsersChangelog();

        // a value past Integer.MAX_VALUE round-trips intact, proving the backing column is genuinely BIGINT -
        // the intentional decoupling from the Integer-typed @Version entity field the migration must honour
        final long beyondIntegerRange = (long) Integer.MAX_VALUE + 1L;
        setVersion("legacy", beyondIntegerRange);

        assertThat(versionOf("legacy")).isEqualTo(beyondIntegerRange);
    }

    @Test
    void migration_isPurelyAdditive_leavesPreExistingColumnsNotNull() throws Exception {
        givenLegacyCustomUserTable();

        runUsersChangelog();

        // the addColumn migration must only add the version column - it must not alter the schema of the
        // pre-existing columns (preservesExistingRowData* asserts row values survive; this asserts the
        // columns themselves remain present and NOT NULL after the upgrade)
        assertThat(columnIsNotNullable("USERNAME")).as("username stays NOT NULL").isTrue();
        assertThat(columnIsNotNullable("EMAIL")).as("email stays NOT NULL").isTrue();
        assertThat(columnIsNotNullable("PASSWORD")).as("password stays NOT NULL").isTrue();
        assertThat(columnIsNotNullable("ROLE")).as("role stays NOT NULL").isTrue();
    }

    private boolean columnIsNotNullable(final String column) throws SQLException {
        try (Connection connection = openConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, "CUSTOM_USER", column)) {
            assertThat(columns.next()).as("column %s exists on custom_user", column).isTrue();
            return "NO".equals(columns.getString("IS_NULLABLE"));
        }
    }

    private int changeSetExecutionCount(final String id, final String author) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID = ? AND AUTHOR = ?")) {
            statement.setString(1, id);
            statement.setString(2, author);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getInt(1);
            }
        }
    }

    private void runUsersChangelog() throws Exception {
        try (Connection connection = openConnection()) {
            final Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    private void rollbackUsersChangelog(final int changeSetsToRollback) throws Exception {
        try (Connection connection = openConnection()) {
            final Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
                liquibase.rollback(changeSetsToRollback, new Contexts(), new LabelExpression());
            }
        }
    }

    private boolean versionColumnExists() throws SQLException {
        try (Connection connection = openConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, "CUSTOM_USER", "VERSION")) {
            return columns.next();
        }
    }

    private void givenLegacyCustomUserTable() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE custom_user ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(100) NOT NULL, "
                    + "email VARCHAR(100) NOT NULL, "
                    + "password VARCHAR(100) NOT NULL, "
                    + "role VARCHAR(100) NOT NULL)");
        }
    }

    private void insertUserWithoutVersion(final String username) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO custom_user (username, email, password, role) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, username);
            statement.setString(2, username + "@gmail.com");
            statement.setString(3, "password");
            statement.setString(4, "ROLE_STUDENT");
            statement.executeUpdate();
        }
    }

    private void setVersion(final String username, final long value) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE custom_user SET version = ? WHERE username = ?")) {
            statement.setLong(1, value);
            statement.setString(2, username);
            statement.executeUpdate();
        }
    }

    private String stringColumnOf(final String username, final String column) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT " + column + " FROM custom_user WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).as("a row exists for %s", username).isTrue();
                return resultSet.getString(column);
            }
        }
    }

    private long versionOf(final String username) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT version FROM custom_user WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).as("a row exists for %s", username).isTrue();
                return resultSet.getLong("version");
            }
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }
}
