package com.educational.platform.config;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import com.educational.platform.common.event.FailedIntegrationEventRecord;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies consistency between the Liquibase changelog ({@code common.yml}) and the
 * JPA entity ({@link FailedIntegrationEventRecord}).
 * <p>
 * This guards against drift between the DDL (changelog) and the ORM mapping (entity).
 * For example, if a developer increases the {@code @Column(length=...)} in the entity
 * but forgets to add a Liquibase migration to ALTER the column, production writes will
 * silently truncate data or fail with a constraint violation.
 * <p>
 * Also verifies the changelog's idempotency precondition for safe re-execution.
 */
class ChangelogSchemaConsistencyTest {

    private static final String CHANGELOG_PATH = "db/common.yml";

    @Test
    void changelog_hasIdempotentPrecondition() throws Exception {
        String content = readChangelog();
        assertThat(content)
                .as("Changelog must have onFail: MARK_RAN precondition for idempotent migration")
                .contains("onFail: MARK_RAN");
        assertThat(content)
                .as("Changelog must check tableExists to prevent duplicate creation")
                .contains("tableExists");
    }

    @Test
    void changelog_tableName_matchesEntityAnnotation() throws Exception {
        String content = readChangelog();
        assertThat(content).contains("tableName: failed_integration_events");
    }

    @Test
    void changelog_eventClassNameColumn_matchesEntityLength() throws Exception {
        String content = readChangelog();
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventClassName");
        Column column = field.getAnnotation(Column.class);

        assertThat(content).contains("name: event_class_name");
        assertThat(content).contains("VARCHAR(" + column.length() + ")");
    }

    @Test
    void changelog_eventPayloadColumn_matchesEntityLength() throws Exception {
        String content = readChangelog();
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventPayload");
        Column column = field.getAnnotation(Column.class);

        assertThat(content).contains("name: event_payload");
        assertThat(content).contains("VARCHAR(" + column.length() + ")");
    }

    @Test
    void changelog_exceptionMessageColumn_matchesEntityLength() throws Exception {
        String content = readChangelog();
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        Column column = field.getAnnotation(Column.class);

        assertThat(content).contains("name: exception_message");
        assertThat(content).contains("VARCHAR(" + column.length() + ")");
    }

    @Test
    void changelog_exceptionClassNameColumn_matchesEntityLength() throws Exception {
        String content = readChangelog();
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionClassName");
        Column column = field.getAnnotation(Column.class);

        assertThat(content).contains("name: exception_class_name");
        assertThat(content).contains("VARCHAR(" + column.length() + ")");
    }

    @Test
    void changelog_idColumn_isBigintAutoIncrement() throws Exception {
        String content = readChangelog();
        assertThat(content).contains("name: id");
        assertThat(content).contains("type: bigint");
        assertThat(content).contains("autoIncrement: true");
    }

    @Test
    void changelog_createdAtColumn_isTimestamp() throws Exception {
        String content = readChangelog();
        assertThat(content).contains("name: created_at");
        assertThat(content).contains("type: TIMESTAMP");
    }

    @Test
    void changelog_retryCountColumn_isInt() throws Exception {
        String content = readChangelog();
        assertThat(content).contains("name: retry_count");
        assertThat(content).contains("type: int");
    }

    @Test
    void changelog_statusColumn_isVarchar20() throws Exception {
        String content = readChangelog();
        assertThat(content).contains("name: status");
        assertThat(content).contains("VARCHAR(20)");
    }

    @Test
    void changelog_nonNullableColumns_matchEntity() throws Exception {
        String content = readChangelog();
        // event_class_name, event_payload, created_at, retry_count, status are NOT NULL
        // exception_message, exception_class_name ARE nullable (no constraint in changelog)
        assertNonNullableInChangelog(content, "event_class_name", true);
        assertNonNullableInChangelog(content, "event_payload", true);
        assertNonNullableInChangelog(content, "created_at", true);
        assertNonNullableInChangelog(content, "retry_count", true);
        assertNonNullableInChangelog(content, "status", true);
    }

    @Test
    void changelog_entityIdGenerationStrategy_matchesAutoIncrement() throws Exception {
        Field idField = FailedIntegrationEventRecord.class.getDeclaredField("id");
        GeneratedValue gv = idField.getAnnotation(GeneratedValue.class);
        assertThat(gv.strategy())
                .as("JPA IDENTITY strategy should map to Liquibase autoIncrement")
                .isEqualTo(GenerationType.IDENTITY);

        String content = readChangelog();
        assertThat(content).contains("autoIncrement: true");
    }

    @Test
    void changelog_hasExactlyEightColumns() throws Exception {
        String content = readChangelog();
        // Count occurrences of "- column:" to verify column count
        long columnCount = content.lines()
                .filter(line -> line.stripLeading().startsWith("- column:"))
                .count();
        assertThat(columnCount)
                .as("Changelog should define exactly 8 columns matching entity fields")
                .isEqualTo(8);
    }

    private void assertNonNullableInChangelog(String content, String columnName, boolean expectedNonNull) {
        // Find the section for this column and verify nullable constraint
        int nameIdx = content.indexOf("name: " + columnName);
        assertThat(nameIdx).as("Column %s must exist in changelog", columnName).isGreaterThan(-1);

        if (expectedNonNull) {
            // The column definition should have a constraints section with nullable: false
            // within a reasonable distance before the column name
            int sectionStart = content.lastIndexOf("- column:", nameIdx);
            String section = content.substring(sectionStart, Math.min(content.length(),
                    nameIdx + columnName.length() + 200));
            assertThat(section)
                    .as("Column %s should have nullable: false constraint", columnName)
                    .contains("nullable: false");
        }
    }

    private String readChangelog() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CHANGELOG_PATH)) {
            assertThat(is).as("Changelog file %s must exist on classpath", CHANGELOG_PATH).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
