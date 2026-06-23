package com.educational.platform.common.event;

import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the JPA entity annotations on {@link FailedIntegrationEventRecord}
 * match the expected Liquibase schema defined in {@code common.yml}.
 * <p>
 * A mismatch between @Column(name=...) and the actual DB column name, or between
 * @Column(length=...) and VARCHAR(...) size, would cause runtime persistence failures
 * that are invisible at compile time and difficult to diagnose.
 */
class FailedIntegrationEventRecordSchemaConsistencyTest {

    @Test
    void tableName_matchesExpectedSchema() {
        Table table = FailedIntegrationEventRecord.class.getAnnotation(Table.class);
        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo("failed_integration_events");
    }

    @Test
    void eventClassNameColumn_nameMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("eventClassName");
        assertThat(column.name()).isEqualTo("event_class_name");
    }

    @Test
    void eventClassNameColumn_isNotNullable() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("eventClassName");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void eventPayloadColumn_nameMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("eventPayload");
        assertThat(column.name()).isEqualTo("event_payload");
    }

    @Test
    void eventPayloadColumn_lengthMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("eventPayload");
        assertThat(column.length()).isEqualTo(4000);
    }

    @Test
    void eventPayloadColumn_isNotNullable() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("eventPayload");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void exceptionMessageColumn_nameMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("exceptionMessage");
        assertThat(column.name()).isEqualTo("exception_message");
    }

    @Test
    void exceptionMessageColumn_lengthMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("exceptionMessage");
        assertThat(column.length()).isEqualTo(2000);
    }

    @Test
    void exceptionMessageColumn_isNullable() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("exceptionMessage");
        assertThat(column.nullable()).isTrue();
    }

    @Test
    void exceptionClassNameColumn_nameMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("exceptionClassName");
        assertThat(column.name()).isEqualTo("exception_class_name");
    }

    @Test
    void createdAtColumn_nameMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("createdAt");
        assertThat(column.name()).isEqualTo("created_at");
    }

    @Test
    void createdAtColumn_isNotNullable() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("createdAt");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void retryCountColumn_nameMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("retryCount");
        assertThat(column.name()).isEqualTo("retry_count");
    }

    @Test
    void retryCountColumn_isNotNullable() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("retryCount");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void statusColumn_nameMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("status");
        assertThat(column.name()).isEqualTo("status");
    }

    @Test
    void statusColumn_isNotNullable() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("status");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void statusField_usesEnumTypeString() throws NoSuchFieldException {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("status");
        Enumerated enumerated = field.getAnnotation(Enumerated.class);
        assertThat(enumerated).isNotNull();
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
    }

    @Test
    void idField_usesIdentityGenerationStrategy() throws NoSuchFieldException {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("id");
        GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
        assertThat(generatedValue).isNotNull();
        assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
    }

    @Test
    void idField_hasIdAnnotation() throws NoSuchFieldException {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("id");
        assertThat(field.getAnnotation(Id.class)).isNotNull();
    }

    @Test
    void allNonIdColumns_haveColumnAnnotation() {
        String[] expectedColumnFields = {
                "eventClassName", "eventPayload", "exceptionMessage",
                "exceptionClassName", "createdAt", "retryCount", "status"
        };
        for (String fieldName : expectedColumnFields) {
            try {
                Field field = FailedIntegrationEventRecord.class.getDeclaredField(fieldName);
                assertThat(field.getAnnotation(Column.class))
                        .as("Field '%s' should have @Column annotation", fieldName)
                        .isNotNull();
            } catch (NoSuchFieldException e) {
                throw new AssertionError("Expected field '" + fieldName + "' not found", e);
            }
        }
    }

    @Test
    void eventClassNameColumn_lengthMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("eventClassName");
        assertThat(column.length()).isEqualTo(500);
    }

    @Test
    void exceptionClassNameColumn_lengthMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("exceptionClassName");
        assertThat(column.length()).isEqualTo(500);
    }

    @Test
    void createdAtField_isInstantType() throws NoSuchFieldException {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("createdAt");
        assertThat(field.getType()).isEqualTo(Instant.class);
    }

    @Test
    void statusField_isStatusEnumType() throws NoSuchFieldException {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("status");
        assertThat(field.getType()).isEqualTo(FailedIntegrationEventRecord.Status.class);
    }

    @Test
    void statusEnumValues_allFitWithinSchemaColumnLength() {
        // The Liquibase schema defines status as VARCHAR(20).
        // If a new enum value exceeds 20 chars, it will cause a runtime data truncation error.
        int schemaColumnLength = 20;
        for (FailedIntegrationEventRecord.Status status : FailedIntegrationEventRecord.Status.values()) {
            assertThat(status.name().length())
                    .as("Status enum value '%s' (length %d) must fit within VARCHAR(%d) defined in common.yml",
                            status.name(), status.name().length(), schemaColumnLength)
                    .isLessThanOrEqualTo(schemaColumnLength);
        }
    }

    @Test
    void eventClassNameColumn_lengthMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("eventClassName");
        assertThat(column.length())
                .as("@Column.length on eventClassName should match VARCHAR(500) in common.yml")
                .isEqualTo(500);
    }

    @Test
    void exceptionClassNameColumn_lengthMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("exceptionClassName");
        assertThat(column.length())
                .as("@Column.length on exceptionClassName should match VARCHAR(500) in common.yml")
                .isEqualTo(500);
    }

    @Test
    void exceptionClassNameColumn_isNullableMatchesSchema() throws NoSuchFieldException {
        Column column = getDeclaredFieldColumn("exceptionClassName");
        assertThat(column.nullable())
                .as("exceptionClassName should be nullable — exception_class_name in common.yml has no NOT NULL constraint")
                .isTrue();
    }

    private Column getDeclaredFieldColumn(String fieldName) throws NoSuchFieldException {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);
        assertThat(column).as("@Column on field '%s'", fieldName).isNotNull();
        return column;
    }
}
