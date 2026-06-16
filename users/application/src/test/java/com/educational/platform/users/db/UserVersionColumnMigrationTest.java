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
    void migration_backfillsExistingRowsToZero() throws Exception {
        givenLegacyCustomUserTable();
        insertUserWithoutVersion("legacy");

        runUsersChangelog();

        assertThat(versionOf("legacy")).isZero();
    }

    @Test
    void migration_newRowWithoutVersion_defaultsToZero() throws Exception {
        givenLegacyCustomUserTable();
        runUsersChangelog();

        insertUserWithoutVersion("fresh");

        assertThat(versionOf("fresh")).isZero();
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

    private void runUsersChangelog() throws Exception {
        try (Connection connection = openConnection()) {
            final Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
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
