package com.educational.platform.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Liquibase migration file {@code common.yml} defines the
 * {@code failed_integration_events} table with the expected columns and types.
 * <p>
 * This catches accidental migration edits that would cause runtime schema
 * mismatches against the {@link com.educational.platform.common.event.FailedIntegrationEventRecord}
 * JPA entity without loading a full Spring context.
 */
class FailedIntegrationEventMigrationConsistencyTest {

    private static final String MIGRATION_PATH = "db/common.yml";
    private static String migrationContent;

    @BeforeAll
    static void loadMigration() throws Exception {
        try (InputStream is = FailedIntegrationEventMigrationConsistencyTest.class
                .getClassLoader().getResourceAsStream(MIGRATION_PATH)) {
            assertThat(is).as("Migration file %s must exist on classpath", MIGRATION_PATH).isNotNull();
            migrationContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void migration_createsFailedIntegrationEventsTable() {
        assertThat(migrationContent).contains("tableName: failed_integration_events");
    }

    @Test
    void migration_hasIdColumnWithAutoIncrement() {
        assertThat(migrationContent).contains("name: id");
        assertThat(migrationContent).contains("type: bigint");
        assertThat(migrationContent).contains("autoIncrement: true");
    }

    @Test
    void migration_hasEventClassNameColumn() {
        assertThat(migrationContent).contains("name: event_class_name");
        assertThat(migrationContent).contains("VARCHAR(500)");
    }

    @Test
    void migration_hasEventPayloadColumn() {
        assertThat(migrationContent).contains("name: event_payload");
        assertThat(migrationContent).contains("VARCHAR(4000)");
    }

    @Test
    void migration_hasExceptionMessageColumn() {
        assertThat(migrationContent).contains("name: exception_message");
        assertThat(migrationContent).contains("VARCHAR(2000)");
    }

    @Test
    void migration_hasExceptionClassNameColumn() {
        assertThat(migrationContent).contains("name: exception_class_name");
    }

    @Test
    void migration_hasCreatedAtColumn() {
        assertThat(migrationContent).contains("name: created_at");
        assertThat(migrationContent).contains("type: TIMESTAMP");
    }

    @Test
    void migration_hasRetryCountColumn() {
        assertThat(migrationContent).contains("name: retry_count");
        assertThat(migrationContent).contains("type: int");
    }

    @Test
    void migration_hasStatusColumn() {
        assertThat(migrationContent).contains("name: status");
        assertThat(migrationContent).contains("VARCHAR(20)");
    }

    @Test
    void migration_hasPrimaryKeyConstraint() {
        assertThat(migrationContent).contains("primaryKey: true");
        assertThat(migrationContent).contains("primaryKeyName: failed_integration_events_pk");
    }

    @Test
    void migration_hasPreConditionToSkipIfTableExists() {
        assertThat(migrationContent).contains("onFail: MARK_RAN");
        assertThat(migrationContent).contains("tableExists");
    }
}
