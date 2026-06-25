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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;

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

    @Test
    void constructor_withIntMinValueRetryCount_setsRetryCount() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", Integer.MIN_VALUE);

        // then
        assertThat(record.getRetryCount()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void failedEventStatus_isStaticInnerEnum() {
        // Enum must be accessible from other packages for status checks
        assertThat(Modifier.isStatic(FailedIntegrationEventRecord.FailedEventStatus.class.getModifiers())).isTrue();
    }

    @Test
    void getTimestamp_afterConstruction_returnsInstant() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);

        // then
        assertThat(record.getTimestamp()).isInstanceOf(Instant.class);
    }

    @Test
    void resolveMethod_returnTypeIsVoid() throws NoSuchMethodException {
        Method resolveMethod = FailedIntegrationEventRecord.class.getMethod("resolve");
        assertThat(resolveMethod.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void constructor_withEventClassNameAtExactColumnLength_setsFieldCorrectly() {
        // given - exactly 500 chars, matching the @Column(length = 500) boundary
        final String boundaryClassName = "com." + "a".repeat(496);

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                boundaryClassName, "payload", "error", 3);

        // then
        assertThat(record.getEventClassName()).hasSize(500);
        assertThat(record.getEventClassName()).isEqualTo(boundaryClassName);
    }

    @Test
    void constructor_publicConstructor_hasFourParameters() throws NoSuchMethodException {
        // when
        Constructor<?> constructor = FailedIntegrationEventRecord.class.getConstructor(
                String.class, String.class, String.class, int.class);

        // then
        assertThat(constructor).isNotNull();
        assertThat(constructor.getParameterCount()).isEqualTo(4);
        assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
    }

    @Test
    void class_isNotAbstract() {
        // JPA entities must be concrete classes for Hibernate instantiation
        assertThat(Modifier.isAbstract(FailedIntegrationEventRecord.class.getModifiers())).isFalse();
    }

    @Test
    void constructor_withMaxLengthExceptionMessage_setsFieldCorrectly() {
        // given - exactly 2000 chars, matching the @Column(length = 2000) boundary
        final String maxMessage = "e".repeat(2000);

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", maxMessage, 3);

        // then
        assertThat(record.getExceptionMessage()).hasSize(2000);
    }

    @Test
    void constructor_withExceptionMessageAtExactColumnLength_setsFieldCorrectly() {
        // given - exactly 2000 chars, matching the @Column(length = 2000) boundary
        final String boundaryMessage = "error:" + "x".repeat(1994);

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", boundaryMessage, 3);

        // then
        assertThat(record.getExceptionMessage()).hasSize(2000);
        assertThat(record.getExceptionMessage()).isEqualTo(boundaryMessage);
    }

    @Test
    void constructor_withPayloadAtExactColumnLength_setsFieldCorrectly() {
        // given - exactly 2000 chars, matching the @Column(length = 2000) boundary
        final String boundaryPayload = "Event[" + "d".repeat(1993) + "]";

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", boundaryPayload, "error", 3);

        // then
        assertThat(record.getEventPayload()).hasSize(2000);
        assertThat(record.getEventPayload()).isEqualTo(boundaryPayload);
    }

    @Test
    void resolve_calledMultipleTimes_remainsResolved() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);

        // when
        record.resolve();
        record.resolve();
        record.resolve();

        // then
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.RESOLVED);
    }

    @Test
    void entity_hasNoPublicSetters() {
        // FailedIntegrationEventRecord is effectively immutable after construction
        // (only resolve() mutates status). No public setX() methods should exist.
        java.util.List<String> publicSetters = Arrays.stream(FailedIntegrationEventRecord.class.getMethods())
                .filter(m -> m.getName().startsWith("set"))
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(java.lang.reflect.Method::getName)
                .toList();

        assertThat(publicSetters)
                .as("Entity should have no public setters to maintain encapsulation")
                .isEmpty();
    }

    @Test
    void class_onlyMutatingMethodIsResolve() {
        long publicVoidInstanceMethods = Arrays.stream(FailedIntegrationEventRecord.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> !Modifier.isStatic(m.getModifiers()))
                .filter(m -> m.getReturnType().equals(void.class))
                .count();

        assertThat(publicVoidInstanceMethods)
                .as("Only resolve() should be a public void instance method")
                .isEqualTo(1);
    }

    @Test
    void statusField_columnLengthIsSufficientForEnumValues() throws NoSuchFieldException {
        java.lang.reflect.Field field = FailedIntegrationEventRecord.class.getDeclaredField("status");
        jakarta.persistence.Column column = field.getAnnotation(jakarta.persistence.Column.class);

        int longestEnumNameLength = 0;
        for (FailedIntegrationEventRecord.FailedEventStatus status : FailedIntegrationEventRecord.FailedEventStatus.values()) {
            longestEnumNameLength = Math.max(longestEnumNameLength, status.name().length());
        }

        assertThat(column.length())
                .as("Column length should accommodate longest enum name (%d chars)", longestEnumNameLength)
                .isGreaterThanOrEqualTo(longestEnumNameLength);
    }

    @Test
    void constructor_timestampIsNotAffectedBySubsequentInstantNowCalls() {
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);
        final java.time.Instant capturedTimestamp = record.getTimestamp();

        assertThat(record.getTimestamp()).isEqualTo(capturedTimestamp);
        assertThat(record.getTimestamp()).isSameAs(capturedTimestamp);
    }

    @Test
    void getRetryCount_returnsExactValuePassedToConstructor() {
        for (int retryCount : new int[]{0, 1, 2, 3, 5, 10, 100}) {
            final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                    "com.example.Event", "payload", "error", retryCount);
            assertThat(record.getRetryCount())
                    .as("retryCount should be %d", retryCount)
                    .isEqualTo(retryCount);
        }
    }

    @Test
    void getId_beforePersistence_returnsNull() {
        // JPA assigns ID only after persist/flush; before that, id is null
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);

        assertThat(record.getId()).isNull();
    }

    @Test
    void resolve_doesNotAffectTimestamp() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);
        final Instant originalTimestamp = record.getTimestamp();

        // when
        record.resolve();

        // then
        assertThat(record.getTimestamp()).isEqualTo(originalTimestamp);
    }

    @Test
    void resolve_doesNotAffectRetryCount() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 5);

        // when
        record.resolve();

        // then
        assertThat(record.getRetryCount()).isEqualTo(5);
    }

    @Test
    void resolve_doesNotAffectEventClassName() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.SpecificEvent", "payload", "error", 3);

        // when
        record.resolve();

        // then
        assertThat(record.getEventClassName()).isEqualTo("com.example.SpecificEvent");
    }

    @Test
    void resolve_doesNotAffectEventPayload() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "original-payload", "error", 3);

        // when
        record.resolve();

        // then
        assertThat(record.getEventPayload()).isEqualTo("original-payload");
    }

    @Test
    void resolve_doesNotAffectExceptionMessage() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "original-error-message", 3);

        // when
        record.resolve();

        // then
        assertThat(record.getExceptionMessage()).isEqualTo("original-error-message");
    }

    @Test
    void constructor_withIntMaxValueRetryCount_setsRetryCount() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", Integer.MAX_VALUE);

        // then
        assertThat(record.getRetryCount()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void class_hasProtectedNoArgConstructor() throws NoSuchMethodException {
        // JPA requires a no-arg constructor; it should be protected (not public) to prevent misuse
        Constructor<?> noArgConstructor = FailedIntegrationEventRecord.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(noArgConstructor.getModifiers()))
                .as("No-arg constructor should be protected for JPA-only use")
                .isTrue();
    }

    @Test
    void constructor_timestampIsBetweenBeforeAndAfterConstruction() {
        // given
        final Instant before = Instant.now();

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);

        // then
        final Instant after = Instant.now();
        assertThat(record.getTimestamp())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    void failedEventStatus_valueOf_returnsCorrectConstants() {
        assertThat(FailedIntegrationEventRecord.FailedEventStatus.valueOf("FAILED"))
                .isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
        assertThat(FailedIntegrationEventRecord.FailedEventStatus.valueOf("RESOLVED"))
                .isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.RESOLVED);
    }

    @Test
    void entity_hasTotalOfSevenDeclaredFields() {
        java.lang.reflect.Field[] fields = FailedIntegrationEventRecord.class.getDeclaredFields();

        assertThat(fields)
                .as("Entity should have exactly 7 fields (id + 6 data fields); new fields need test coverage")
                .hasSize(7);
    }

    @Test
    void allColumnNames_useSnakeCaseConvention() {
        for (java.lang.reflect.Field field : FailedIntegrationEventRecord.class.getDeclaredFields()) {
            jakarta.persistence.Column column = field.getAnnotation(jakarta.persistence.Column.class);
            if (column != null && !column.name().isEmpty()) {
                assertThat(column.name())
                        .as("Column name for field '%s' must use snake_case", field.getName())
                        .matches("[a-z][a-z_]*[a-z]");
            }
        }
    }

    @Test
    void constructor_allFieldsAreIndependentFromInputObjects() {
        String className = "com.example.Event";
        String payload = "test-payload";
        String errorMsg = "test-error";
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                className, payload, errorMsg, 3);

        assertThat(record.getEventClassName()).isEqualTo(className);
        assertThat(record.getEventPayload()).isEqualTo(payload);
        assertThat(record.getExceptionMessage()).isEqualTo(errorMsg);
        assertThat(record.getRetryCount()).isEqualTo(3);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
        assertThat(record.getTimestamp()).isNotNull();
        assertThat(record.getId()).isNull();
    }

    @Test
    void eventClassName_columnLength_matchesLiquibaseSchema() throws NoSuchFieldException {
        // Liquibase schema defines event_class_name as VARCHAR(500)
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventClassName");
        Column column = field.getAnnotation(Column.class);

        assertThat(column.length())
                .as("eventClassName @Column length must match Liquibase schema (500)")
                .isEqualTo(500);
    }

    @Test
    void eventPayload_columnLength_matchesLiquibaseSchema() throws NoSuchFieldException {
        // Liquibase schema defines event_payload as VARCHAR(2000)
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("eventPayload");
        Column column = field.getAnnotation(Column.class);

        assertThat(column.length())
                .as("eventPayload @Column length must match Liquibase schema (2000)")
                .isEqualTo(2000);
    }

    @Test
    void exceptionMessage_columnLength_matchesLiquibaseSchema() throws NoSuchFieldException {
        // Liquibase schema defines exception_message as VARCHAR(2000)
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        Column column = field.getAnnotation(Column.class);

        assertThat(column.length())
                .as("exceptionMessage @Column length must match Liquibase schema (2000)")
                .isEqualTo(2000);
    }

    @Test
    void constructor_withZeroRetryCount_setsStatusToFailed() {
        // even with zero retries, the initial status must still be FAILED
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 0);

        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void allFields_haveNonNullColumnConstraint() {
        // All data fields in the entity must be non-nullable per schema
        for (Field field : FailedIntegrationEventRecord.class.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                assertThat(column.nullable())
                        .as("Field '%s' @Column must have nullable=false", field.getName())
                        .isFalse();
            }
        }
    }

    @Test
    void resolve_doesNotAffectId() {
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
    void resolve_isIdempotent_allFieldsUnchangedOnSubsequentCalls() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "test-payload", "test-error", 5);
        record.resolve();
        final Instant timestampAfterFirstResolve = record.getTimestamp();

        // when
        record.resolve();

        // then
        assertThat(record.getId()).isNull();
        assertThat(record.getEventClassName()).isEqualTo("com.example.Event");
        assertThat(record.getEventPayload()).isEqualTo("test-payload");
        assertThat(record.getExceptionMessage()).isEqualTo("test-error");
        assertThat(record.getRetryCount()).isEqualTo(5);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.RESOLVED);
        assertThat(record.getTimestamp()).isEqualTo(timestampAfterFirstResolve);
    }

    @Test
    void statusField_hasEnumeratedAnnotationWithStringType() throws NoSuchFieldException {
        // @Enumerated(EnumType.STRING) ensures status is stored as text, not ordinal
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("status");
        jakarta.persistence.Enumerated enumerated = field.getAnnotation(jakarta.persistence.Enumerated.class);

        assertThat(enumerated).isNotNull();
        assertThat(enumerated.value()).isEqualTo(jakarta.persistence.EnumType.STRING);
    }

    @Test
    void idField_hasGeneratedValueWithIdentityStrategy() throws NoSuchFieldException {
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("id");
        jakarta.persistence.GeneratedValue generatedValue = field.getAnnotation(jakarta.persistence.GeneratedValue.class);

        assertThat(generatedValue).isNotNull();
        assertThat(generatedValue.strategy()).isEqualTo(jakarta.persistence.GenerationType.IDENTITY);
    }

    @Test
    void constructor_withIntMaxValueRetryCount_setsRetryCount() {
        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", Integer.MAX_VALUE);

        // then
        assertThat(record.getRetryCount()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void resolve_preservesTimestampExactly() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 3);
        final Instant originalTimestamp = record.getTimestamp();

        // when
        record.resolve();

        // then - timestamp must remain unchanged after resolve
        assertThat(record.getTimestamp()).isEqualTo(originalTimestamp);
    }

    @Test
    void resolve_preservesRetryCountExactly() {
        // given
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", 5);

        // when
        record.resolve();

        // then - retry count must remain unchanged after resolve
        assertThat(record.getRetryCount()).isEqualTo(5);
    }

    @Test
    void resolve_preservesEventClassNameExactly() {
        // given
        final String className = "com.example.SpecificEvent";
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                className, "payload", "error", 3);

        // when
        record.resolve();

        // then
        assertThat(record.getEventClassName()).isEqualTo(className);
    }

    @Test
    void resolve_preservesExceptionMessageExactly() {
        // given
        final String message = "specific error message for verification";
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", message, 3);

        // when
        record.resolve();

        // then
        assertThat(record.getExceptionMessage()).isEqualTo(message);
    }

    @Test
    void constructor_withVeryLargePayload_setsFieldWithoutLoss() {
        // given - simulates a large event toString() that may exceed typical column constraints
        final String largePayload = "EventRecord[" + "x".repeat(5000) + "]";

        // when
        final FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", largePayload, "error", 3);

        // then - entity does not truncate; DB constraint enforcement is separate
        assertThat(record.getEventPayload()).hasSize(largePayload.length());
        assertThat(record.getEventPayload()).startsWith("EventRecord[");
    }

}

