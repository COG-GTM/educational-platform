package com.educational.platform.common.event;

import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FailedIntegrationEventRecordTest {

    @Test
    void constructor_setsAllFieldsCorrectly() throws Exception {
        // given
        String eventClassName = "com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent";
        String eventPayload = "SendCourseToApproveIntegrationEvent[courseId=123e4567-e89b-12d3-a456-426655440001]";
        String exceptionMessage = "DB connection lost";
        String exceptionClassName = "org.springframework.dao.DataAccessResourceFailureException";
        int retryCount = 3;

        Instant before = Instant.now();

        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                eventClassName, eventPayload, exceptionMessage, exceptionClassName, retryCount);

        Instant after = Instant.now();

        // then
        assertThat(getField(record, "eventClassName")).isEqualTo(eventClassName);
        assertThat(getField(record, "eventPayload")).isEqualTo(eventPayload);
        assertThat(getField(record, "exceptionMessage")).isEqualTo(exceptionMessage);
        assertThat(getField(record, "exceptionClassName")).isEqualTo(exceptionClassName);
        assertThat((int) getField(record, "retryCount")).isEqualTo(retryCount);
        assertThat((FailedIntegrationEventRecord.Status) getField(record, "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);

        Instant createdAt = (Instant) getField(record, "createdAt");
        assertThat(createdAt).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void constructor_nullExceptionMessage_accepted() throws Exception {
        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", null, "java.lang.RuntimeException", 3);

        // then
        assertThat(getField(record, "exceptionMessage")).isNull();
        assertThat(getField(record, "eventClassName")).isEqualTo("com.example.Event");
    }

    @Test
    void constructor_zeroRetryCount_accepted() throws Exception {
        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 0);

        // then
        assertThat((int) getField(record, "retryCount")).isZero();
    }

    @Test
    void constructor_statusDefaultsToFailed() throws Exception {
        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);

        // then
        assertThat((FailedIntegrationEventRecord.Status) getField(record, "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void constructor_createdAtIsSetToCurrentTime() throws Exception {
        // given
        Instant before = Instant.now();

        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);

        // then
        Instant after = Instant.now();
        Instant createdAt = (Instant) getField(record, "createdAt");
        assertThat(createdAt).isBetween(before, after);
    }

    @Test
    void statusEnum_containsExpectedValues() {
        // then
        assertThat(FailedIntegrationEventRecord.Status.values())
                .containsExactly(FailedIntegrationEventRecord.Status.FAILED, FailedIntegrationEventRecord.Status.RESOLVED);
    }

    @Test
    void constructor_nullExceptionClassName_accepted() throws Exception {
        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", null, 3);

        // then
        assertThat(getField(record, "exceptionClassName")).isNull();
        assertThat(getField(record, "eventClassName")).isEqualTo("com.example.Event");
    }

    @Test
    void constructor_emptyStrings_accepted() throws Exception {
        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "", "", "", "", 3);

        // then
        assertThat(getField(record, "eventClassName")).isEqualTo("");
        assertThat(getField(record, "eventPayload")).isEqualTo("");
        assertThat(getField(record, "exceptionMessage")).isEqualTo("");
        assertThat(getField(record, "exceptionClassName")).isEqualTo("");
    }

    @Test
    void protectedNoArgConstructor_existsForJpa() throws Exception {
        // JPA requires a no-arg constructor
        Constructor<FailedIntegrationEventRecord> constructor =
                FailedIntegrationEventRecord.class.getDeclaredConstructor();
        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();

        constructor.setAccessible(true);
        FailedIntegrationEventRecord record = constructor.newInstance();
        assertThat(record).isNotNull();
    }

    @Test
    void constructor_negativeRetryCount_accepted() throws Exception {
        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", -1);

        // then
        assertThat((int) getField(record, "retryCount")).isEqualTo(-1);
    }

    @Test
    void constructor_idIsNullBeforePersistence() throws Exception {
        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);

        // then
        assertThat(getField(record, "id")).isNull();
    }

    @Test
    void class_hasEntityAnnotation() {
        assertThat(FailedIntegrationEventRecord.class.getAnnotation(Entity.class)).isNotNull();
    }

    @Test
    void class_hasTableAnnotation_withCorrectName() {
        Table table = FailedIntegrationEventRecord.class.getAnnotation(Table.class);
        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo("failed_integration_events");
    }

    @Test
    void idField_hasGeneratedValueWithIdentityStrategy() throws Exception {
        Field idField = FailedIntegrationEventRecord.class.getDeclaredField("id");
        assertThat(idField.getAnnotation(Id.class)).isNotNull();
        GeneratedValue gv = idField.getAnnotation(GeneratedValue.class);
        assertThat(gv).isNotNull();
        assertThat(gv.strategy()).isEqualTo(GenerationType.IDENTITY);
    }

    @Test
    void eventPayloadField_hasColumnWithLength4000() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventPayload");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.length()).isEqualTo(4000);
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void exceptionMessageField_hasColumnWithLength2000() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.length()).isEqualTo(2000);
    }

    @Test
    void statusField_hasEnumeratedStringAnnotation() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("status");
        Enumerated enumerated = field.getAnnotation(Enumerated.class);
        assertThat(enumerated).isNotNull();
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
    }

    @Test
    void createdAtField_isNotNullable() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("createdAt");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void retryCountField_isNotNullable() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("retryCount");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void constructor_largePayloadNearLimit_accepted() throws Exception {
        String largePayload = "x".repeat(3999);
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", largePayload, "error", "java.lang.RuntimeException", 3);

        assertThat(getField(record, "eventPayload")).isEqualTo(largePayload);
    }

    @Test
    void statusEnum_valueOf_fromString() {
        assertThat(FailedIntegrationEventRecord.Status.valueOf("FAILED"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
        assertThat(FailedIntegrationEventRecord.Status.valueOf("RESOLVED"))
                .isEqualTo(FailedIntegrationEventRecord.Status.RESOLVED);
    }

    @Test
    void eventClassNameField_hasCorrectColumnName() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventClassName");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("event_class_name");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void exceptionClassNameField_hasColumnAnnotation() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionClassName");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("exception_class_name");
    }

    @Test
    void statusField_hasCorrectColumnName() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("status");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("status");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void constructor_withMaxIntRetryCount_accepted() throws Exception {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", Integer.MAX_VALUE);

        assertThat((int) getField(record, "retryCount")).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void constructor_longExceptionMessage_nearLimit_accepted() throws Exception {
        String longMessage = "x".repeat(1999);
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", longMessage, "java.lang.RuntimeException", 3);

        assertThat(getField(record, "exceptionMessage")).isEqualTo(longMessage);
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
