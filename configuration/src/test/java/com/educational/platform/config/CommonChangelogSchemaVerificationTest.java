package com.educational.platform.config;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the structure of the {@code common.yml} Liquibase changeset that creates
 * the {@code failed_integration_events} table. These tests guard against accidental
 * modifications to the schema definition that would break dead-letter persistence
 * at runtime.
 */
class CommonChangelogSchemaVerificationTest {

    private static final String COMMON_CHANGELOG_PATH = "db/common.yml";

    @Test
    void commonChangelog_existsOnClasspath() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(COMMON_CHANGELOG_PATH)) {
            assertThat(is)
                    .as("common.yml must be on the classpath for Liquibase to create the failed_integration_events table")
                    .isNotNull();
        } catch (Exception e) {
            throw new AssertionError("Failed to read common.yml", e);
        }
    }

    @Test
    void commonChangelog_hasIdempotentPrecondition() throws Exception {
        String content = readChangelog();

        assertThat(content)
                .as("common.yml must have a preCondition with onFail: MARK_RAN to be idempotent")
                .contains("preConditions")
                .contains("onFail: MARK_RAN")
                .contains("tableExists");
    }

    @Test
    void commonChangelog_createsFailedIntegrationEventsTable() throws Exception {
        String content = readChangelog();

        assertThat(content)
                .as("common.yml must create the failed_integration_events table")
                .contains("createTable")
                .contains("tableName: failed_integration_events");
    }

    @Test
    void commonChangelog_idColumnHasAutoIncrement() throws Exception {
        String content = readChangelog();

        assertThat(content)
                .as("id column must use autoIncrement for IDENTITY generation strategy")
                .contains("autoIncrement: true");
    }

    @Test
    void commonChangelog_idColumnIsPrimaryKey() throws Exception {
        String content = readChangelog();

        assertThat(content)
                .as("id column must be the primary key")
                .contains("primaryKey: true");
    }

    @Test
    void commonChangelog_hasAllExpectedColumns() throws Exception {
        String content = readChangelog();

        assertThat(content)
                .contains("name: id")
                .contains("name: event_class_name")
                .contains("name: event_payload")
                .contains("name: exception_message")
                .contains("name: exception_class_name")
                .contains("name: created_at")
                .contains("name: retry_count")
                .contains("name: status");
    }

    @Test
    void commonChangelog_eventClassNameIsVarchar500() throws Exception {
        String content = readChangelog();

        assertThat(content).contains("type: VARCHAR(500)");
        // Verify the 500-length VARCHAR appears at least twice (event_class_name + exception_class_name)
        int count = countOccurrences(content, "VARCHAR(500)");
        assertThat(count)
                .as("Both event_class_name and exception_class_name should be VARCHAR(500)")
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void commonChangelog_eventPayloadIsVarchar4000() throws Exception {
        String content = readChangelog();

        assertThat(content)
                .as("event_payload column must be VARCHAR(4000)")
                .contains("type: VARCHAR(4000)");
    }

    @Test
    void commonChangelog_exceptionMessageIsVarchar2000() throws Exception {
        String content = readChangelog();

        assertThat(content)
                .as("exception_message column must be VARCHAR(2000)")
                .contains("type: VARCHAR(2000)");
    }

    @Test
    void commonChangelog_statusIsVarchar20() throws Exception {
        String content = readChangelog();

        assertThat(content)
                .as("status column must be VARCHAR(20)")
                .contains("type: VARCHAR(20)");
    }

    @Test
    void commonChangelog_createdAtIsTimestamp() throws Exception {
        String content = readChangelog();

        assertThat(content)
                .as("created_at column must be TIMESTAMP type")
                .contains("type: TIMESTAMP");
    }

    private String readChangelog() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(COMMON_CHANGELOG_PATH)) {
            assertThat(is).as("common.yml must exist on classpath").isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(substring, idx)) != -1) {
            count++;
            idx += substring.length();
        }
        return count;
    }
}
