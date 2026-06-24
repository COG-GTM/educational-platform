package com.educational.platform.common.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FailedIntegrationEventRecordTest {

    @Test
    void constructor_setsAllFieldsCorrectly() {
        // given
        final String eventClassName = "com.example.SomeIntegrationEvent";
        final String eventPayload = "SomeIntegrationEvent[id=123]";
        final String exceptionMessage = "DB connection lost";
        final int retryCount = 3;

        // when
        final Instant before = Instant.now();
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                eventClassName, eventPayload, exceptionMessage, retryCount);
        final Instant after = Instant.now();

        // then
        assertThat(record.getEventClassName()).isEqualTo(eventClassName);
        assertThat(record.getEventPayload()).isEqualTo(eventPayload);
        assertThat(record.getExceptionMessage()).isEqualTo(exceptionMessage);
        assertThat(record.getRetryCount()).isEqualTo(retryCount);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
        assertThat(record.getTimestamp()).isNotNull();
        assertThat(record.getTimestamp()).isBetween(before, after);
        assertThat(record.getId()).isNull();
    }

    @Test
    void resolve_changesStatusFromFailedToResolved() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);

        // when
        record.resolve();

        // then
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.RESOLVED);
    }

    @Test
    void resolve_calledTwice_remainsResolved() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);

        // when
        record.resolve();
        record.resolve();

        // then
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.RESOLVED);
    }

    @Test
    void constructor_withZeroRetryCount_setsRetryCountToZero() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 0);

        // then
        assertThat(record.getRetryCount()).isZero();
    }

    @Test
    void constructor_withEmptyStrings_setsFieldsToEmptyStrings() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "", "", "", 1);

        // then
        assertThat(record.getEventClassName()).isEmpty();
        assertThat(record.getEventPayload()).isEmpty();
        assertThat(record.getExceptionMessage()).isEmpty();
    }

    @Test
    void constructor_withMaxLengthPayload_setsPayloadCorrectly() {
        // given
        final String maxPayload = "x".repeat(2000);

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", maxPayload, "error", 3);

        // then
        assertThat(record.getEventPayload()).hasSize(2000);
    }

    @Test
    void constructor_setsTimestampCloseToNow() {
        // when
        final Instant before = Instant.now();
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);
        final Instant after = Instant.now();

        // then
        assertThat(record.getTimestamp()).isAfterOrEqualTo(before);
        assertThat(record.getTimestamp()).isBeforeOrEqualTo(after);
    }

    @Test
    void failedEventStatus_hasExpectedValues() {
        // then
        assertThat(FailedIntegrationEventRecord.FailedEventStatus.values())
                .containsExactly(
                        FailedIntegrationEventRecord.FailedEventStatus.FAILED,
                        FailedIntegrationEventRecord.FailedEventStatus.RESOLVED
                );
    }

    @Test
    void constructor_withNegativeRetryCount_setsRetryCount() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", -1);

        // then
        assertThat(record.getRetryCount()).isEqualTo(-1);
    }

    @Test
    void defaultConstructor_createsInstanceWithNullFields() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord();

        // then
        assertThat(record.getId()).isNull();
        assertThat(record.getEventClassName()).isNull();
        assertThat(record.getEventPayload()).isNull();
        assertThat(record.getExceptionMessage()).isNull();
        assertThat(record.getTimestamp()).isNull();
        assertThat(record.getRetryCount()).isZero();
        assertThat(record.getStatus()).isNull();
    }

    @Test
    void constructor_withNullExceptionMessage_setsFieldToNull() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", null, 3);

        // then
        assertThat(record.getExceptionMessage()).isNull();
        assertThat(record.getEventClassName()).isEqualTo("com.example.Event");
        assertThat(record.getEventPayload()).isEqualTo("payload");
    }

    @Test
    void constructor_withLargeRetryCount_setsRetryCount() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", Integer.MAX_VALUE);

        // then
        assertThat(record.getRetryCount()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void resolve_doesNotAffectOtherFields() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error msg", 5);
        final Instant timestampBefore = record.getTimestamp();

        // when
        record.resolve();

        // then
        assertThat(record.getEventClassName()).isEqualTo("com.example.Event");
        assertThat(record.getEventPayload()).isEqualTo("payload");
        assertThat(record.getExceptionMessage()).isEqualTo("error msg");
        assertThat(record.getRetryCount()).isEqualTo(5);
        assertThat(record.getTimestamp()).isEqualTo(timestampBefore);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.RESOLVED);
    }

    @Test
    void class_hasEntityAnnotation() {
        // then
        assertThat(FailedIntegrationEventRecord.class.isAnnotationPresent(Entity.class)).isTrue();
    }

    @Test
    void class_hasTableAnnotationWithCorrectName() {
        // when
        Table table = FailedIntegrationEventRecord.class.getAnnotation(Table.class);

        // then
        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo("failed_integration_events");
    }

    @Test
    void idField_hasIdAndGeneratedValueAnnotations() throws NoSuchFieldException {
        // when
        Field idField = FailedIntegrationEventRecord.class.getDeclaredField("id");

        // then
        assertThat(idField.isAnnotationPresent(Id.class)).isTrue();
        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
        assertThat(generatedValue).isNotNull();
        assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
    }

    @Test
    void eventPayloadField_hasColumnAnnotationWithCorrectLength() throws NoSuchFieldException {
        // when
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventPayload");
        Column column = field.getAnnotation(Column.class);

        // then
        assertThat(column).isNotNull();
        assertThat(column.nullable()).isFalse();
        assertThat(column.length()).isEqualTo(2000);
    }

    @Test
    void exceptionMessageField_hasColumnAnnotationWithCorrectLength() throws NoSuchFieldException {
        // when
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        Column column = field.getAnnotation(Column.class);

        // then
        assertThat(column).isNotNull();
        assertThat(column.nullable()).isFalse();
        assertThat(column.length()).isEqualTo(2000);
    }

    @Test
    void statusField_hasEnumeratedStringAnnotation() throws NoSuchFieldException {
        // when
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("status");
        Enumerated enumerated = field.getAnnotation(Enumerated.class);

        // then
        assertThat(enumerated).isNotNull();
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
    }

    @Test
    void requiredFields_haveNonNullableColumnAnnotations() throws NoSuchFieldException {
        // then
        for (String fieldName : new String[]{"eventClassName", "eventPayload", "exceptionMessage", "timestamp", "retryCount", "status"}) {
            Field field = FailedIntegrationEventRecord.class.getDeclaredField(fieldName);
            Column column = field.getAnnotation(Column.class);
            assertThat(column).as("Column annotation for field '%s'", fieldName).isNotNull();
            assertThat(column.nullable()).as("nullable for field '%s'", fieldName).isFalse();
        }
    }

    @Test
    void eventClassNameField_hasColumnAnnotationWithCorrectName() throws NoSuchFieldException {
        // when
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventClassName");
        Column column = field.getAnnotation(Column.class);

        // then
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("event_class_name");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void timestampField_hasColumnAnnotationWithCorrectName() throws NoSuchFieldException {
        // when
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("timestamp");
        Column column = field.getAnnotation(Column.class);

        // then
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("timestamp");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void retryCountField_hasColumnAnnotationWithCorrectName() throws NoSuchFieldException {
        // when
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("retryCount");
        Column column = field.getAnnotation(Column.class);

        // then
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("retry_count");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void columnNameMappings_areCorrectForAllFields() throws NoSuchFieldException {
        // then
        assertThat(getColumnName("eventClassName")).isEqualTo("event_class_name");
        assertThat(getColumnName("eventPayload")).isEqualTo("event_payload");
        assertThat(getColumnName("exceptionMessage")).isEqualTo("exception_message");
        assertThat(getColumnName("timestamp")).isEqualTo("timestamp");
        assertThat(getColumnName("retryCount")).isEqualTo("retry_count");
        assertThat(getColumnName("status")).isEqualTo("status");
    }

    @Test
    void constructor_withNullEventClassName_setsFieldToNull() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                null, "payload", "error", 3);

        // then
        assertThat(record.getEventClassName()).isNull();
        assertThat(record.getEventPayload()).isEqualTo("payload");
        assertThat(record.getExceptionMessage()).isEqualTo("error");
    }

    @Test
    void constructor_withNullEventPayload_setsFieldToNull() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", null, "error", 3);

        // then
        assertThat(record.getEventPayload()).isNull();
        assertThat(record.getEventClassName()).isEqualTo("com.example.Event");
        assertThat(record.getExceptionMessage()).isEqualTo("error");
    }

    @Test
    void constructor_withAllNullStrings_setsAllToNull() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                null, null, null, 0);

        // then
        assertThat(record.getEventClassName()).isNull();
        assertThat(record.getEventPayload()).isNull();
        assertThat(record.getExceptionMessage()).isNull();
        assertThat(record.getRetryCount()).isZero();
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
        assertThat(record.getTimestamp()).isNotNull();
    }

    private String getColumnName(String fieldName) throws NoSuchFieldException {
        return FailedIntegrationEventRecord.class.getDeclaredField(fieldName).getAnnotation(Column.class).name();
    }

    @Test
    void failedEventStatus_valueOf_roundTrip() {
        // then - valueOf(name()) should return the same enum constant
        for (FailedIntegrationEventRecord.FailedEventStatus status : FailedIntegrationEventRecord.FailedEventStatus.values()) {
            assertThat(FailedIntegrationEventRecord.FailedEventStatus.valueOf(status.name())).isEqualTo(status);
        }
    }

    @Test
    void failedEventStatus_failedName_matchesDatabaseValue() {
        // Enum is stored as STRING in database, so name() must match expected DB column value
        // then
        assertThat(FailedIntegrationEventRecord.FailedEventStatus.FAILED.name()).isEqualTo("FAILED");
        assertThat(FailedIntegrationEventRecord.FailedEventStatus.RESOLVED.name()).isEqualTo("RESOLVED");
    }

    @Test
    void resolve_fromDefaultConstructor_setsStatusToResolved() {
        // given - default constructor leaves status as null
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord();
        assertThat(record.getStatus()).isNull();

        // when - resolve sets status to RESOLVED regardless of previous state
        record.resolve();

        // then
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.RESOLVED);
    }

    @Test
    void constructor_withSpecialCharactersInPayload_setsFieldCorrectly() {
        // given
        final String specialPayload = "Event{id='test', data=\"json: {\\\"key\\\": \\\"value\\\"}\"}";

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", specialPayload, "error", 3);

        // then
        assertThat(record.getEventPayload()).isEqualTo(specialPayload);
    }

    @Test
    void constructor_withUnicodeInExceptionMessage_setsFieldCorrectly() {
        // given
        final String unicodeMessage = "Ошибка подключения к базе данных";

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", unicodeMessage, 3);

        // then
        assertThat(record.getExceptionMessage()).isEqualTo(unicodeMessage);
    }

    @Test
    void defaultConstructor_isProtected() throws NoSuchMethodException {
        // JPA requires a non-private no-arg constructor, but it should not be public
        // when
        Constructor<?> constructor = FailedIntegrationEventRecord.class.getDeclaredConstructor();

        // then
        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void constructor_multipleInstances_haveIndependentState() {
        // given
        final FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload1", "error1", 3);
        final FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload2", "error2", 5);

        // when
        record1.resolve();

        // then
        assertThat(record1.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.RESOLVED);
        assertThat(record2.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
        assertThat(record1.getEventPayload()).isNotEqualTo(record2.getEventPayload());
    }

    @Test
    void resolve_idRemainsNullWithoutPersistence() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);
        assertThat(record.getId()).isNull();

        // when
        record.resolve();

        // then
        assertThat(record.getId()).isNull();
    }

    @Test
    void eventPayloadField_hasCorrectColumnName() throws NoSuchFieldException {
        // when
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventPayload");
        Column column = field.getAnnotation(Column.class);

        // then
        assertThat(column.name()).isEqualTo("event_payload");
    }

    @Test
    void exceptionMessageField_hasCorrectColumnName() throws NoSuchFieldException {
        // when
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        Column column = field.getAnnotation(Column.class);

        // then
        assertThat(column.name()).isEqualTo("exception_message");
    }

    @Test
    void statusField_hasCorrectColumnName() throws NoSuchFieldException {
        // when
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("status");
        Column column = field.getAnnotation(Column.class);

        // then
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("status");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void allGetters_arePublic() throws NoSuchMethodException {
        for (String methodName : new String[]{"getId", "getEventClassName", "getEventPayload",
                "getExceptionMessage", "getTimestamp", "getRetryCount", "getStatus"}) {
            // when
            java.lang.reflect.Method method = FailedIntegrationEventRecord.class.getMethod(methodName);

            // then
            assertThat(Modifier.isPublic(method.getModifiers()))
                    .as("Method '%s' should be public", methodName)
                    .isTrue();
        }
    }

    @Test
    void resolveMethod_isPublic() throws NoSuchMethodException {
        // when
        java.lang.reflect.Method method = FailedIntegrationEventRecord.class.getMethod("resolve");

        // then
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void constructor_withWhitespacePayload_preservesWhitespace() {
        // given
        final String whitespacePayload = "  \t\n  ";

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", whitespacePayload, "error", 3);

        // then
        assertThat(record.getEventPayload()).isEqualTo(whitespacePayload);
    }

    @Test
    void constructor_multipleConsecutiveInstances_haveDistinctTimestamps() throws InterruptedException {
        // when
        final FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);
        Thread.sleep(5);
        final FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);

        // then
        assertThat(record2.getTimestamp()).isAfterOrEqualTo(record1.getTimestamp());
    }

    @Test
    void constructor_withPayloadExceedingColumnLength_setsFieldWithoutTruncation() {
        // Entity does not enforce column length at construction time; DB layer enforces it.
        // given
        final String longPayload = "x".repeat(2001);

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", longPayload, "error", 3);

        // then
        assertThat(record.getEventPayload()).hasSize(2001);
    }

    @Test
    void constructor_withExceptionMessageExceedingColumnLength_setsFieldWithoutTruncation() {
        // given
        final String longMessage = "e".repeat(2001);

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", longMessage, 3);

        // then
        assertThat(record.getExceptionMessage()).hasSize(2001);
    }

    @Test
    void constructor_withEventClassNameContainingNestedClassNotation_setsCorrectly() {
        // given - anonymous/inner class names use $ notation
        final String nestedClassName = "com.example.Outer$Inner$DeepNested";

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                nestedClassName, "payload", "error", 3);

        // then
        assertThat(record.getEventClassName()).isEqualTo(nestedClassName);
    }

    @Test
    void resolve_thenGetStatus_returnsResolvedEnumConstant() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);

        // when
        record.resolve();

        // then - verify enum identity, not just equality
        assertThat(record.getStatus()).isSameAs(FailedIntegrationEventRecord.FailedEventStatus.RESOLVED);
    }

    @Test
    void constructor_setsStatusToFailedEnumConstant() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);

        // then - verify enum identity
        assertThat(record.getStatus()).isSameAs(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void constructor_withRetryCountOfOne_setsCorrectly() {
        // Boundary: minimum meaningful retry count
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 1);

        // then
        assertThat(record.getRetryCount()).isEqualTo(1);
    }

    @Test
    void id_field_isPrivateAndGeneratedByJpa() throws NoSuchFieldException {
        // when
        Field idField = FailedIntegrationEventRecord.class.getDeclaredField("id");

        // then
        assertThat(Modifier.isPrivate(idField.getModifiers())).isTrue();
        assertThat(idField.getType()).isEqualTo(Long.class);
    }

    @Test
    void timestampField_isInstantType() throws NoSuchFieldException {
        // when
        Field timestampField = FailedIntegrationEventRecord.class.getDeclaredField("timestamp");

        // then
        assertThat(timestampField.getType()).isEqualTo(Instant.class);
    }

    @Test
    void statusField_isFailedEventStatusType() throws NoSuchFieldException {
        // when
        Field statusField = FailedIntegrationEventRecord.class.getDeclaredField("status");

        // then
        assertThat(statusField.getType()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.class);
    }

    @Test
    void retryCountField_isIntType() throws NoSuchFieldException {
        // when
        Field retryCountField = FailedIntegrationEventRecord.class.getDeclaredField("retryCount");

        // then
        assertThat(retryCountField.getType()).isEqualTo(int.class);
    }

    @Test
    void allEntityFields_arePrivate() throws NoSuchFieldException {
        for (String fieldName : new String[]{"id", "eventClassName", "eventPayload",
                "exceptionMessage", "timestamp", "retryCount", "status"}) {
            Field field = FailedIntegrationEventRecord.class.getDeclaredField(fieldName);
            assertThat(Modifier.isPrivate(field.getModifiers()))
                    .as("Field '%s' should be private", fieldName)
                    .isTrue();
        }
    }

    @Test
    void failedEventStatus_isPublicEnum() {
        // then
        assertThat(FailedIntegrationEventRecord.FailedEventStatus.class.isEnum()).isTrue();
        assertThat(Modifier.isPublic(FailedIntegrationEventRecord.FailedEventStatus.class.getModifiers())).isTrue();
    }

    @Test
    void class_isNotFinal() {
        // JPA entities must not be final for Hibernate proxy creation
        assertThat(Modifier.isFinal(FailedIntegrationEventRecord.class.getModifiers())).isFalse();
    }

    @Test
    void constructor_withMaxLengthClassName_setsFieldCorrectly() {
        // given
        final String maxClassName = "x".repeat(500);

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                maxClassName, "payload", "error", 3);

        // then
        assertThat(record.getEventClassName()).hasSize(500);
    }

    @Test
    void failedEventStatus_hasTwoConstants() {
        assertThat(FailedIntegrationEventRecord.FailedEventStatus.values())
                .containsExactlyInAnyOrder(
                        FailedIntegrationEventRecord.FailedEventStatus.FAILED,
                        FailedIntegrationEventRecord.FailedEventStatus.RESOLVED);
    }

    @Test
    void constructor_twoRecordsWithSameData_areNotEqual() {
        // JPA entities use reference equality, not value equality
        // given
        final FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);
        final FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);

        // then
        assertThat(record1).isNotEqualTo(record2);
        assertThat(record1).isNotSameAs(record2);
    }

    @Test
    void constructor_withEventClassNameExceedingColumnLength_setsFieldWithoutTruncation() {
        // given
        final String longClassName = "com.example." + "a".repeat(501);

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                longClassName, "payload", "error", 3);

        // then
        assertThat(record.getEventClassName()).hasSize(longClassName.length());
        assertThat(record.getEventClassName()).isEqualTo(longClassName);
    }

    @Test
    void eventClassNameField_hasColumnAnnotationWithCorrectLength() throws NoSuchFieldException {
        // when
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventClassName");
        Column column = field.getAnnotation(Column.class);

        // then
        assertThat(column).isNotNull();
        assertThat(column.length()).isEqualTo(500);
    }

}

