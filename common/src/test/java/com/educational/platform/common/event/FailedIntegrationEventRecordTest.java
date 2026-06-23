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

    @Test
    void constructor_bothNullableFieldsNull_accepted() throws Exception {
        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", null, null, 3);

        // then
        assertThat(getField(record, "exceptionMessage")).isNull();
        assertThat(getField(record, "exceptionClassName")).isNull();
        assertThat(getField(record, "eventClassName")).isEqualTo("com.example.Event");
        assertThat(getField(record, "eventPayload")).isEqualTo("payload");
        assertThat((int) getField(record, "retryCount")).isEqualTo(3);
        assertThat((FailedIntegrationEventRecord.Status) getField(record, "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void constructor_unicodeStrings_accepted() throws Exception {
        // given
        String eventClassName = "com.example.\u00c9v\u00e9ntHandler";
        String eventPayload = "Payload with \u00fc\u00f1\u00ee\u00e7\u00f8\u00f0\u00e9 characters: \u4e16\u754c";
        String exceptionMessage = "Erreur: \u00e9chec de la connexion \u00e0 la base de donn\u00e9es";
        String exceptionClassName = "com.example.\u00c9xception";

        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                eventClassName, eventPayload, exceptionMessage, exceptionClassName, 3);

        // then
        assertThat(getField(record, "eventClassName")).isEqualTo(eventClassName);
        assertThat(getField(record, "eventPayload")).isEqualTo(eventPayload);
        assertThat(getField(record, "exceptionMessage")).isEqualTo(exceptionMessage);
        assertThat(getField(record, "exceptionClassName")).isEqualTo(exceptionClassName);
    }

    @Test
    void constructor_multipleInstances_haveIndependentTimestamps() throws Exception {
        // when
        FailedIntegrationEventRecord first = new FailedIntegrationEventRecord(
                "com.example.Event", "payload1", "error1", "java.lang.RuntimeException", 1);
        FailedIntegrationEventRecord second = new FailedIntegrationEventRecord(
                "com.example.Event", "payload2", "error2", "java.lang.RuntimeException", 2);

        // then
        Instant firstCreatedAt = (Instant) getField(first, "createdAt");
        Instant secondCreatedAt = (Instant) getField(second, "createdAt");
        assertThat(firstCreatedAt).isBeforeOrEqualTo(secondCreatedAt);
        assertThat(getField(first, "eventPayload")).isNotEqualTo(getField(second, "eventPayload"));
        assertThat((int) getField(first, "retryCount")).isNotEqualTo((int) getField(second, "retryCount"));
    }

    @Test
    void constructor_payloadAtExactLimit_accepted() throws Exception {
        // given
        String exactLimitPayload = "x".repeat(4000);

        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", exactLimitPayload, "error", "java.lang.RuntimeException", 3);

        // then
        assertThat(getField(record, "eventPayload")).isEqualTo(exactLimitPayload);
        assertThat(((String) getField(record, "eventPayload")).length()).isEqualTo(4000);
    }

    @Test
    void constructor_exceptionMessageAtExactLimit_accepted() throws Exception {
        // given
        String exactLimitMessage = "x".repeat(2000);

        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", exactLimitMessage, "java.lang.RuntimeException", 3);

        // then
        assertThat(getField(record, "exceptionMessage")).isEqualTo(exactLimitMessage);
        assertThat(((String) getField(record, "exceptionMessage")).length()).isEqualTo(2000);
    }

    @Test
    void protectedNoArgConstructor_fieldsAreNull() throws Exception {
        // given
        Constructor<FailedIntegrationEventRecord> constructor =
                FailedIntegrationEventRecord.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // when
        FailedIntegrationEventRecord record = constructor.newInstance();

        // then
        assertThat(getField(record, "id")).isNull();
        assertThat(getField(record, "eventClassName")).isNull();
        assertThat(getField(record, "eventPayload")).isNull();
        assertThat(getField(record, "exceptionMessage")).isNull();
        assertThat(getField(record, "exceptionClassName")).isNull();
        assertThat(getField(record, "createdAt")).isNull();
        assertThat(getField(record, "status")).isNull();
        assertThat((int) getField(record, "retryCount")).isZero();
    }

    @Test
    void constructor_multipleInstancesHaveIndependentState() throws Exception {
        FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.EventA", "payloadA", "errorA", "java.lang.RuntimeException", 1);
        FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.EventB", "payloadB", "errorB", "java.lang.IllegalStateException", 5);

        assertThat(getField(record1, "eventClassName")).isEqualTo("com.example.EventA");
        assertThat(getField(record2, "eventClassName")).isEqualTo("com.example.EventB");
        assertThat((int) getField(record1, "retryCount")).isEqualTo(1);
        assertThat((int) getField(record2, "retryCount")).isEqualTo(5);
    }

    @Test
    void eventPayloadField_hasCorrectColumnName() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventPayload");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("event_payload");
    }

    @Test
    void exceptionMessageField_hasCorrectColumnName() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("exception_message");
    }

    @Test
    void exceptionMessageField_isNullable() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.nullable()).isTrue();
    }

    @Test
    void exceptionClassNameField_isNullable() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionClassName");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.nullable()).isTrue();
    }

    @Test
    void eventClassNameField_isNotNullable() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventClassName");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.nullable())
                .as("eventClassName must be NOT NULL — dead-letter records without an event class are undiagnosable")
                .isFalse();
    }

    @Test
    void eventPayloadField_isNotNullable() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventPayload");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.nullable())
                .as("eventPayload must be NOT NULL — dead-letter records without payload cannot be replayed")
                .isFalse();
    }

    @Test
    void statusField_isNotNullable() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("status");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.nullable())
                .as("status must be NOT NULL — every dead-letter record requires a lifecycle state")
                .isFalse();
    }

    @Test
    void createdAtField_hasCorrectColumnName() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("createdAt");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("created_at");
    }

    @Test
    void retryCountField_hasCorrectColumnName() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("retryCount");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("retry_count");
    }

    @Test
    void allFields_arePrivate() throws Exception {
        for (Field field : FailedIntegrationEventRecord.class.getDeclaredFields()) {
            assertThat(Modifier.isPrivate(field.getModifiers()))
                    .as("Field '%s' should be private", field.getName())
                    .isTrue();
        }
    }

    @Test
    void className_doesNotEndWithEvent() {
        // Named *Record (not *Event) to satisfy the ArchUnit rule
        // requiring all *Event classes to be immutable.
        assertThat(FailedIntegrationEventRecord.class.getSimpleName()).endsWith("Record");
        assertThat(FailedIntegrationEventRecord.class.getSimpleName()).doesNotEndWith("Event");
    }

    @Test
    void class_isNotFinal() {
        // JPA entities should not be final (Hibernate proxy requirement)
        assertThat(Modifier.isFinal(FailedIntegrationEventRecord.class.getModifiers())).isFalse();
    }

    @Test
    void class_hasNoPublicGetters() {
        // Encapsulated entity — no public getters exposed
        long publicGetterCount = java.util.Arrays.stream(FailedIntegrationEventRecord.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> m.getName().startsWith("get") || m.getName().startsWith("is"))
                .count();
        assertThat(publicGetterCount).isZero();
    }

    @Test
    void class_hasNoPublicSetters() {
        long publicSetterCount = java.util.Arrays.stream(FailedIntegrationEventRecord.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> m.getName().startsWith("set"))
                .count();
        assertThat(publicSetterCount).as("Dead-letter records should be immutable — no public setters").isZero();
    }

    @Test
    void constructor_nullEventClassName_doesNotThrowAtJavaLevel() throws Exception {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                null, "payload", "error", "java.lang.RuntimeException", 3);

        assertThat(getField(record, "eventClassName")).isNull();
        assertThat(getField(record, "eventPayload")).isEqualTo("payload");
    }

    @Test
    void constructor_nullEventPayload_doesNotThrowAtJavaLevel() throws Exception {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", null, "error", "java.lang.RuntimeException", 3);

        assertThat(getField(record, "eventPayload")).isNull();
        assertThat(getField(record, "eventClassName")).isEqualTo("com.example.Event");
    }

    @Test
    void class_hasExpectedNumberOfFields() {
        Field[] fields = FailedIntegrationEventRecord.class.getDeclaredFields();
        assertThat(fields).as("Guard against accidental field additions/removals").hasSize(8);
    }

    @Test
    void publicConstructor_hasFiveParameters() throws NoSuchMethodException {
        java.lang.reflect.Constructor<FailedIntegrationEventRecord> ctor =
                FailedIntegrationEventRecord.class.getConstructor(
                        String.class, String.class, String.class, String.class, int.class);
        assertThat(ctor.getParameterCount()).isEqualTo(5);
    }

    @Test
    void class_hasExactlyTwoConstructors() {
        java.lang.reflect.Constructor<?>[] constructors = FailedIntegrationEventRecord.class.getDeclaredConstructors();
        assertThat(constructors).as("One protected no-arg (JPA) + one public 5-arg").hasSize(2);
    }

    @Test
    void constructor_withMinIntRetryCount_accepted() throws Exception {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", Integer.MIN_VALUE);

        assertThat((int) getField(record, "retryCount")).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void idField_isBoxedLongType() throws NoSuchFieldException {
        Field idField = FailedIntegrationEventRecord.class.getDeclaredField("id");
        assertThat(idField.getType()).isEqualTo(Long.class);
    }

    @Test
    void statusEnum_hasExactlyTwoValues() {
        assertThat(FailedIntegrationEventRecord.Status.values()).hasSize(2);
    }

    @Test
    void createdAtField_isInstantType() throws NoSuchFieldException {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("createdAt");
        assertThat(field.getType()).isEqualTo(Instant.class);
    }

    @Test
    void retryCountField_isPrimitiveIntType() throws NoSuchFieldException {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("retryCount");
        assertThat(field.getType()).isEqualTo(int.class);
    }

    @Test
    void statusField_isEnumType() throws NoSuchFieldException {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("status");
        assertThat(field.getType().isEnum()).isTrue();
        assertThat(field.getType()).isEqualTo(FailedIntegrationEventRecord.Status.class);
    }

    @Test
    void eventClassNameField_hasColumnLength500() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventClassName");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.length())
                .as("eventClassName length must match Liquibase migration VARCHAR(500)")
                .isEqualTo(500);
    }

    @Test
    void exceptionClassNameField_hasColumnLength500() throws Exception {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionClassName");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.length())
                .as("exceptionClassName length must match Liquibase migration VARCHAR(500)")
                .isEqualTo(500);
    }

    @Test
    void constructor_allNonNullFieldsPopulated_noFieldIsNull() throws Exception {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);

        assertThat(getField(record, "eventClassName")).isNotNull();
        assertThat(getField(record, "eventPayload")).isNotNull();
        assertThat(getField(record, "exceptionMessage")).isNotNull();
        assertThat(getField(record, "exceptionClassName")).isNotNull();
        assertThat(getField(record, "createdAt")).isNotNull();
        assertThat(getField(record, "status")).isNotNull();
    }

    @Test
    void statusEnum_failedOrdinal_isZero() {
        assertThat(FailedIntegrationEventRecord.Status.FAILED.ordinal()).isZero();
    }

    @Test
    void statusEnum_resolvedOrdinal_isOne() {
        assertThat(FailedIntegrationEventRecord.Status.RESOLVED.ordinal()).isEqualTo(1);
    }

    @Test
    void constructor_payloadExceedingColumnLimit_acceptedAtJavaLevel() throws Exception {
        String overLimitPayload = "x".repeat(5000);
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", overLimitPayload, "error", "java.lang.RuntimeException", 3);

        assertThat(getField(record, "eventPayload")).isEqualTo(overLimitPayload);
        assertThat(((String) getField(record, "eventPayload")).length()).isEqualTo(5000);
    }

    @Test
    void constructor_exceptionMessageExceedingColumnLimit_acceptedAtJavaLevel() throws Exception {
        String overLimitMessage = "x".repeat(3000);
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", overLimitMessage, "java.lang.RuntimeException", 3);

        assertThat(getField(record, "exceptionMessage")).isEqualTo(overLimitMessage);
        assertThat(((String) getField(record, "exceptionMessage")).length()).isEqualTo(3000);
    }

    @Test
    void allFieldsExceptId_haveColumnAnnotation() throws Exception {
        String[] fieldsWithColumn = {"eventClassName", "eventPayload", "exceptionMessage",
                "exceptionClassName", "createdAt", "retryCount", "status"};
        for (String fieldName : fieldsWithColumn) {
            Field field = FailedIntegrationEventRecord.class.getDeclaredField(fieldName);
            assertThat(field.getAnnotation(Column.class))
                    .as("Field '%s' should have @Column annotation", fieldName)
                    .isNotNull();
        }
    }

    @Test
    void constructor_whitespaceOnlyStrings_accepted() throws Exception {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "   ", "\t\n", " ", "  ", 3);

        assertThat(getField(record, "eventClassName")).isEqualTo("   ");
        assertThat(getField(record, "eventPayload")).isEqualTo("\t\n");
        assertThat(getField(record, "exceptionMessage")).isEqualTo(" ");
        assertThat(getField(record, "exceptionClassName")).isEqualTo("  ");
    }

    @Test
    void constructor_allFieldsNull_except_retryCount_accepted() throws Exception {
        // when — all nullable String fields set to null simultaneously
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                null, null, null, null, 0);

        // then
        assertThat(getField(record, "eventClassName")).isNull();
        assertThat(getField(record, "eventPayload")).isNull();
        assertThat(getField(record, "exceptionMessage")).isNull();
        assertThat(getField(record, "exceptionClassName")).isNull();
        assertThat((int) getField(record, "retryCount")).isZero();
        assertThat(getField(record, "createdAt")).isNotNull();
        assertThat(getField(record, "status")).isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void statusEnum_failedName_matchesExpectedString() {
        assertThat(FailedIntegrationEventRecord.Status.FAILED.name()).isEqualTo("FAILED");
    }

    @Test
    void statusEnum_resolvedName_matchesExpectedString() {
        assertThat(FailedIntegrationEventRecord.Status.RESOLVED.name()).isEqualTo("RESOLVED");
    }

    @Test
    void constructor_specialCharactersInPayload_preserved() throws Exception {
        // given — payloads may contain record toString() output with brackets, quotes, etc.
        String payload = "Event[courseId=123e4567-e89b-12d3-a456-426655440001, rating=4.5]";
        String message = "Error: \"column 'name' cannot be null\" at org.h2.jdbc";

        // when
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", payload, message, "org.h2.jdbc.JdbcSQLException", 3);

        // then
        assertThat(getField(record, "eventPayload")).isEqualTo(payload);
        assertThat(getField(record, "exceptionMessage")).isEqualTo(message);
    }

    @Test
    void class_isPublic() {
        assertThat(Modifier.isPublic(FailedIntegrationEventRecord.class.getModifiers())).isTrue();
    }

    @Test
    void constructor_longEventClassName_nearColumnLimit_accepted() throws Exception {
        // given — eventClassName column is VARCHAR(500) per Liquibase migration
        String longClassName = "com.educational.platform." + "a".repeat(475);
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                longClassName, "payload", "error", "java.lang.RuntimeException", 3);

        assertThat(getField(record, "eventClassName")).isEqualTo(longClassName);
        assertThat(((String) getField(record, "eventClassName")).length()).isEqualTo(500);
    }

    @Test
    void constructor_longExceptionClassName_nearColumnLimit_accepted() throws Exception {
        // given — exceptionClassName column is VARCHAR(500) per Liquibase migration
        String longExceptionClass = "org.springframework.dao." + "a".repeat(476);  // 24 + 476 = 500
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", longExceptionClass, 3);

        assertThat(getField(record, "exceptionClassName")).isEqualTo(longExceptionClass);
        assertThat(((String) getField(record, "exceptionClassName")).length()).isEqualTo(500);
    }

    @Test
    void class_isNotAbstract() {
        assertThat(Modifier.isAbstract(FailedIntegrationEventRecord.class.getModifiers())).isFalse();
    }

    @Test
    void statusEnum_isPublicInnerEnum() {
        assertThat(Modifier.isPublic(FailedIntegrationEventRecord.Status.class.getModifiers())).isTrue();
        assertThat(FailedIntegrationEventRecord.Status.class.isEnum()).isTrue();
    }

    @Test
    void statusEnum_isDeclaredInsideRecord() {
        assertThat(FailedIntegrationEventRecord.Status.class.getDeclaringClass())
                .as("Status enum should be a member class of FailedIntegrationEventRecord")
                .isEqualTo(FailedIntegrationEventRecord.class);
    }

    @Test
    void constructor_withVeryLongExceptionClassName_acceptedAtJavaLevel() throws Exception {
        String longClassName = "com.educational.platform." + "a".repeat(200) + ".SomeException";

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", longClassName, 3);

        assertThat(getField(record, "exceptionClassName")).isEqualTo(longClassName);
    }

    @Test
    void twoInstances_withIdenticalData_areNotEqual() {
        FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "com.example.Exception", 3);
        FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "com.example.Exception", 3);

        assertThat(record1)
                .as("JPA entities use identity equality (Object.equals), not value equality")
                .isNotEqualTo(record2);
        assertThat(record1).isNotSameAs(record2);
    }

    @Test
    void publicConstructor_parameterTypes_inOrder() throws NoSuchMethodException {
        Constructor<?> ctor = FailedIntegrationEventRecord.class.getConstructor(
                String.class, String.class, String.class, String.class, int.class);
        Class<?>[] paramTypes = ctor.getParameterTypes();
        assertThat(paramTypes[0]).as("eventClassName").isEqualTo(String.class);
        assertThat(paramTypes[1]).as("eventPayload").isEqualTo(String.class);
        assertThat(paramTypes[2]).as("exceptionMessage").isEqualTo(String.class);
        assertThat(paramTypes[3]).as("exceptionClassName").isEqualTo(String.class);
        assertThat(paramTypes[4]).as("retryCount").isEqualTo(int.class);
    }

    @Test
    void constructor_emptyExceptionMessage_accepted() throws Exception {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "", "com.example.Exception", 3);
        assertThat(getField(record, "exceptionMessage")).isEqualTo("");
    }

    @Test
    void protectedNoArgConstructor_isNotPublic() throws Exception {
        Constructor<?> noArgCtor = FailedIntegrationEventRecord.class.getDeclaredConstructor();
        assertThat(Modifier.isProtected(noArgCtor.getModifiers()))
                .as("No-arg constructor should be protected (JPA use only, not public API)")
                .isTrue();
        assertThat(Modifier.isPublic(noArgCtor.getModifiers())).isFalse();
    }

    @Test
    void idField_isNotInsertable_byConstructor() throws Exception {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "com.example.Exception", 3);
        assertThat(getField(record, "id"))
                .as("id should be null before JPA persistence (generated by DB)")
                .isNull();
    }

    @Test
    void constructor_statusIsNeverResolved() throws Exception {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);
        FailedIntegrationEventRecord.Status status =
                (FailedIntegrationEventRecord.Status) getField(record, "status");
        assertThat(status)
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED)
                .isNotEqualTo(FailedIntegrationEventRecord.Status.RESOLVED);
    }

    @Test
    void statusField_hasNoPublicSetter() {
        long publicSetterCount = java.util.Arrays.stream(
                        FailedIntegrationEventRecord.class.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .filter(m -> m.getName().equals("setStatus"))
                .count();
        assertThat(publicSetterCount)
                .as("Status should not be publicly modifiable")
                .isZero();
    }

    @Test
    void resolvedStatus_stringRepresentation_matchesEnumName() {
        assertThat(FailedIntegrationEventRecord.Status.RESOLVED.name())
                .isEqualTo("RESOLVED");
    }

    @Test
    void constructor_eventClassName_exceedingDefaultColumnLimit_acceptedAtJavaLevel() throws Exception {
        // given — event class name well beyond VARCHAR(255) — accepted by Java, fails at DB
        String exceedingClassName = "com.educational.platform.very.deeply.nested.package." + "a".repeat(500);

        // when
        var record = new FailedIntegrationEventRecord(
                exceedingClassName, "payload", "message", "exClassName", 3);

        // then — Java object creation succeeds regardless of column limit
        assertThat(getField(record, "eventClassName")).isEqualTo(exceedingClassName);
        assertThat(((String) getField(record, "eventClassName")).length()).isGreaterThan(500);
    }

    @Test
    void constructor_exceptionClassName_exceedingDefaultColumnLimit_acceptedAtJavaLevel() throws Exception {
        // given — exception class name well beyond VARCHAR(255) — accepted by Java, fails at DB
        String exceedingExClassName = "org.springframework.dao.very.specific." + "b".repeat(500);

        // when
        var record = new FailedIntegrationEventRecord(
                "eventClass", "payload", "message", exceedingExClassName, 3);

        // then — Java object creation succeeds regardless of column limit
        assertThat(getField(record, "exceptionClassName")).isEqualTo(exceedingExClassName);
        assertThat(((String) getField(record, "exceptionClassName")).length()).isGreaterThan(500);
    }

    @Test
    void constructor_eventPayload_exceedingColumnLimit_acceptedAtJavaLevel() throws Exception {
        // given — event payload well beyond VARCHAR(4000) — accepted by Java, fails at DB
        String exceedingPayload = "p".repeat(5000);

        // when
        var record = new FailedIntegrationEventRecord(
                "com.example.Event", exceedingPayload, "message", "exClassName", 3);

        // then — Java object creation succeeds regardless of column limit
        assertThat(getField(record, "eventPayload")).isEqualTo(exceedingPayload);
        assertThat(((String) getField(record, "eventPayload")).length()).isGreaterThan(4000);
    }

    @Test
    void constructor_exceptionMessage_exceedingColumnLimit_acceptedAtJavaLevel() throws Exception {
        // given — exception message well beyond VARCHAR(2000) — accepted by Java, fails at DB
        String exceedingMessage = "m".repeat(3000);

        // when
        var record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", exceedingMessage, "exClassName", 3);

        // then — Java object creation succeeds regardless of column limit
        assertThat(getField(record, "exceptionMessage")).isEqualTo(exceedingMessage);
        assertThat(((String) getField(record, "exceptionMessage")).length()).isGreaterThan(2000);
    }

    @Test
    void constructor_createdAt_isNotBeforeConstructionStartTime() throws Exception {
        // given
        Instant before = Instant.now();

        // when
        var record = new FailedIntegrationEventRecord(
                "className", "payload", "message", "exClassName", 3);

        // then
        Instant createdAt = (Instant) getField(record, "createdAt");
        assertThat(createdAt).isAfterOrEqualTo(before);
        assertThat(createdAt).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void constructor_allStringFieldsAtMaxColumnLimits_accepted() throws Exception {
        // given — all string fields simultaneously at their column limits
        String eventClassName = "A".repeat(500);
        String eventPayload = "B".repeat(4000);
        String exceptionMessage = "C".repeat(2000);
        String exceptionClassName = "D".repeat(500);

        // when
        var record = new FailedIntegrationEventRecord(
                eventClassName, eventPayload, exceptionMessage, exceptionClassName, 3);

        // then
        assertThat(getField(record, "eventClassName")).isEqualTo(eventClassName);
        assertThat(getField(record, "eventPayload")).isEqualTo(eventPayload);
        assertThat(getField(record, "exceptionMessage")).isEqualTo(exceptionMessage);
        assertThat(getField(record, "exceptionClassName")).isEqualTo(exceptionClassName);
        assertThat((int) getField(record, "retryCount")).isEqualTo(3);
        assertThat(getField(record, "status")).isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void constructor_rapidSuccessiveCreation_producesIndependentRecordsWithNonDecreasingTimestamps() throws Exception {
        // when — create multiple records in rapid succession
        var record1 = new FailedIntegrationEventRecord("class1", "payload1", "msg1", "exClass1", 1);
        var record2 = new FailedIntegrationEventRecord("class2", "payload2", "msg2", "exClass2", 2);
        var record3 = new FailedIntegrationEventRecord("class3", "payload3", "msg3", "exClass3", 3);

        // then — timestamps are monotonically non-decreasing
        Instant t1 = (Instant) getField(record1, "createdAt");
        Instant t2 = (Instant) getField(record2, "createdAt");
        Instant t3 = (Instant) getField(record3, "createdAt");
        assertThat(t2).isAfterOrEqualTo(t1);
        assertThat(t3).isAfterOrEqualTo(t2);

        // then — fields are independent
        assertThat(getField(record1, "eventClassName")).isNotEqualTo(getField(record2, "eventClassName"));
        assertThat(getField(record2, "retryCount")).isNotEqualTo(getField(record3, "retryCount"));
    }

    @Test
    void constructor_statusIsAlwaysFailed_neverResolved() throws Exception {
        // when — construct with various parameters
        var record = new FailedIntegrationEventRecord(
                "eventClass", "payload", "message", "exClass", 5);

        // then — constructor always sets FAILED; there is no way to set RESOLVED via constructor
        assertThat(getField(record, "status")).isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
        assertThat(getField(record, "status")).isNotEqualTo(FailedIntegrationEventRecord.Status.RESOLVED);
    }

    @Test
    void constructor_allFieldsExceedColumnLimitsSimultaneously_acceptedAtJavaLevel() throws Exception {
        // given — all string fields exceed their column limits simultaneously
        String eventClassName = "E".repeat(1000);    // exceeds 500
        String eventPayload = "F".repeat(10000);     // exceeds 4000
        String exceptionMessage = "G".repeat(5000);  // exceeds 2000
        String exceptionClassName = "H".repeat(1000); // exceeds 500

        // when
        var record = new FailedIntegrationEventRecord(
                eventClassName, eventPayload, exceptionMessage, exceptionClassName, 3);

        // then — Java object creation succeeds even when all fields exceed DB limits
        assertThat(((String) getField(record, "eventClassName")).length()).isEqualTo(1000);
        assertThat(((String) getField(record, "eventPayload")).length()).isEqualTo(10000);
        assertThat(((String) getField(record, "exceptionMessage")).length()).isEqualTo(5000);
        assertThat(((String) getField(record, "exceptionClassName")).length()).isEqualTo(1000);
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
