package com.educational.platform.common.event;

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

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
