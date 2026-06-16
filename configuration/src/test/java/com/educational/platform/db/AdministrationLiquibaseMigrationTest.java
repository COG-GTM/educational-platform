package com.educational.platform.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.junit.jupiter.api.Test;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * Verifies the administration Liquibase changelog ({@code db/administration.yml}) provisions the
 * optimistic-locking {@code version} column introduced by this PR.
 *
 * <p>This is the only place the Liquibase migration is actually executed against a database in the
 * test suite. The application module's {@code @DataJpaTest} exercises the <em>Hibernate-generated</em>
 * schema, while the booted monolith runs with {@code ddl-auto=none} and relies entirely on this
 * changelog. Without this test the second half of the feature (the {@code addColumn} changeSet) would
 * have no direct coverage.
 */
class AdministrationLiquibaseMigrationTest {

    private static final String CHANGELOG = "db/administration.yml";

    @Test
    void migration_courseProposal_versionColumnIsNonNullBigintWithZeroDefault() throws Exception {
        final String url = migratedDatabaseUrl();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                ResultSet columns = connection.getMetaData()
                        .getColumns(null, null, "COURSE_PROPOSAL", "VERSION")) {

            assertThat(columns.next()).as("version column exists on course_proposal").isTrue();
            assertThat(columns.getInt("DATA_TYPE")).isEqualTo(Types.BIGINT);
            assertThat(columns.getString("TYPE_NAME")).isEqualToIgnoringCase("BIGINT");
            assertThat(columns.getString("IS_NULLABLE")).isEqualTo("NO");
            assertThat(columns.getString("COLUMN_DEF")).contains("0");
        }
    }

    @Test
    void migration_rowInsertedWithoutVersion_defaultsToZero() throws Exception {
        final String url = migratedDatabaseUrl();

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            // mirrors the web module's insert_data.sql seed which omits the version column; the
            // changeSet's "default 0 not null" must populate it so the optimistic-lock UPDATE works
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "insert into course_proposal (uuid, status) "
                                + "values ('123e4567-e89b-12d3-a456-426655440001', 'APPROVED')");
            }

            try (Statement statement = connection.createStatement();
                    ResultSet row = statement.executeQuery("select version from course_proposal")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getLong("version")).isZero();
            }
        }
    }

    /**
     * Creates a uniquely named in-memory H2 database, applies the administration changelog to it and
     * returns the JDBC url. {@code DB_CLOSE_DELAY=-1} keeps the schema alive after the migration
     * connection is closed so the assertions can reopen the same database.
     */
    private static String migratedDatabaseUrl() throws Exception {
        final String url = "jdbc:h2:mem:administration_liquibase_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            final Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to apply administration changelog", e);
        }

        return url;
    }
}
