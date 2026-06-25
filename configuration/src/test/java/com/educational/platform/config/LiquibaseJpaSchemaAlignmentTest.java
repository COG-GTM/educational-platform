package com.educational.platform.config;

import com.educational.platform.common.event.FailedIntegrationEventRecord;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the JPA entity annotations on {@link FailedIntegrationEventRecord} are
 * consistent with the Liquibase migration in {@code common/src/main/resources/db/common.yml}.
 *
 * This test catches drift between the migration script and entity definition, such as:
 * - Renamed columns that aren't reflected in @Column(name=...)
 * - Nullability mismatches between DB constraint and @Column(nullable=false)
 * - Length mismatches between VARCHAR(n) and @Column(length=n)
 */
class LiquibaseJpaSchemaAlignmentTest {

    @Test
    void entity_tableNameMatchesLiquibaseMigration() {
        // Liquibase: tableName: failed_integration_events
        Table table = FailedIntegrationEventRecord.class.getAnnotation(Table.class);

        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo("failed_integration_events");
    }

    @Test
    void entity_idColumnUsesIdentityStrategy() throws NoSuchFieldException {
        // Liquibase: autoIncrement: true
        Field idField = FailedIntegrationEventRecord.class.getDeclaredField("id");
        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);

        assertThat(generatedValue).isNotNull();
        assertThat(generatedValue.strategy())
                .as("autoIncrement in Liquibase maps to IDENTITY strategy in JPA")
                .isEqualTo(GenerationType.IDENTITY);
    }

    @Test
    void entity_eventClassNameColumnMatchesLiquibase() throws NoSuchFieldException {
        // Liquibase: name: event_class_name, type: VARCHAR(500), nullable: false
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventClassName");
        Column column = field.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("event_class_name");
        assertThat(column.nullable()).isFalse();
        assertThat(column.length())
                .as("JPA @Column length must match Liquibase VARCHAR(500)")
                .isEqualTo(500);
    }

    @Test
    void entity_eventPayloadColumnMatchesLiquibase() throws NoSuchFieldException {
        // Liquibase: name: event_payload, type: VARCHAR(2000), nullable: false
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventPayload");
        Column column = field.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("event_payload");
        assertThat(column.nullable()).isFalse();
        assertThat(column.length())
                .as("JPA @Column length must match Liquibase VARCHAR(2000)")
                .isEqualTo(2000);
    }

    @Test
    void entity_exceptionMessageColumnMatchesLiquibase() throws NoSuchFieldException {
        // Liquibase: name: exception_message, type: VARCHAR(2000), nullable: false
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        Column column = field.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("exception_message");
        assertThat(column.nullable()).isFalse();
        assertThat(column.length())
                .as("JPA @Column length must match Liquibase VARCHAR(2000)")
                .isEqualTo(2000);
    }

    @Test
    void entity_timestampColumnMatchesLiquibase() throws NoSuchFieldException {
        // Liquibase: name: timestamp, type: TIMESTAMP, nullable: false
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("timestamp");
        Column column = field.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("timestamp");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void entity_retryCountColumnMatchesLiquibase() throws NoSuchFieldException {
        // Liquibase: name: retry_count, type: int, nullable: false
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("retryCount");
        Column column = field.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("retry_count");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void entity_statusColumnMatchesLiquibase() throws NoSuchFieldException {
        // Liquibase: name: status, type: VARCHAR(50), nullable: false
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("status");
        Column column = field.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("status");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void entity_statusEnumValues_fitWithinLiquibaseVarchar50() {
        // Liquibase: status column is VARCHAR(50); enum values stored as strings must fit
        for (FailedIntegrationEventRecord.FailedEventStatus status :
                FailedIntegrationEventRecord.FailedEventStatus.values()) {
            assertThat(status.name().length())
                    .as("Enum value '%s' (%d chars) must fit within VARCHAR(50)",
                            status.name(), status.name().length())
                    .isLessThanOrEqualTo(50);
        }
    }

    @Test
    void entity_hasCorrectNumberOfPersistentColumns() {
        // Liquibase defines 7 columns: id, event_class_name, event_payload,
        // exception_message, timestamp, retry_count, status
        long columnAnnotatedFields = java.util.Arrays.stream(
                        FailedIntegrationEventRecord.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class) || f.isAnnotationPresent(Id.class))
                .count();

        assertThat(columnAnnotatedFields)
                .as("Entity persistent field count must match Liquibase column count (7)")
                .isEqualTo(7);
    }

    @Test
    void entity_isMarkedAsJpaEntity() {
        assertThat(FailedIntegrationEventRecord.class.isAnnotationPresent(Entity.class))
                .as("Class must be annotated with @Entity for JPA")
                .isTrue();
    }

    @Test
    void entity_idFieldType_matchesLiquibaseBigint() throws NoSuchFieldException {
        // Liquibase: type: bigint -> Java: Long
        Field idField = FailedIntegrationEventRecord.class.getDeclaredField("id");

        assertThat(idField.getType())
                .as("bigint in Liquibase maps to Long in JPA entity")
                .isEqualTo(Long.class);
    }

    @Test
    void entity_retryCountFieldType_matchesLiquibaseInt() throws NoSuchFieldException {
        // Liquibase: type: int -> Java: int (primitive)
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("retryCount");

        assertThat(field.getType())
                .as("int in Liquibase maps to int primitive in JPA entity")
                .isEqualTo(int.class);
    }

    @Test
    void entity_timestampFieldType_isInstant() throws NoSuchFieldException {
        // Liquibase: type: TIMESTAMP -> Java: Instant
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("timestamp");

        assertThat(field.getType())
                .as("TIMESTAMP in Liquibase maps to Instant in JPA entity")
                .isEqualTo(java.time.Instant.class);
    }
}
