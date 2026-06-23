package com.educational.platform.common.event;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link FailedIntegrationEventRecord} construction with ALL string fields
 * simultaneously at their maximum column lengths.
 * <p>
 * Individual field-limit tests exist elsewhere; this test verifies that no unexpected
 * interaction occurs when all fields are large simultaneously (e.g., memory pressure,
 * object size issues, or hidden string interning behavior).
 */
class FailedIntegrationEventRecordCombinedBoundaryTest {

    @Test
    void constructor_allStringFieldsAtExactColumnLimits_accepted() throws Exception {
        // Column limits from @Column annotations
        String eventClassName = "x".repeat(500);      // @Column(length = 500)
        String eventPayload = "y".repeat(4000);       // @Column(length = 4000)
        String exceptionMessage = "z".repeat(2000);   // @Column(length = 2000)
        String exceptionClassName = "w".repeat(500);  // @Column(length = 500)
        int retryCount = 3;

        Instant before = Instant.now();
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                eventClassName, eventPayload, exceptionMessage, exceptionClassName, retryCount);
        Instant after = Instant.now();

        assertThat(getField(record, "eventClassName")).isEqualTo(eventClassName);
        assertThat(((String) getField(record, "eventClassName")).length()).isEqualTo(500);
        assertThat(getField(record, "eventPayload")).isEqualTo(eventPayload);
        assertThat(((String) getField(record, "eventPayload")).length()).isEqualTo(4000);
        assertThat(getField(record, "exceptionMessage")).isEqualTo(exceptionMessage);
        assertThat(((String) getField(record, "exceptionMessage")).length()).isEqualTo(2000);
        assertThat(getField(record, "exceptionClassName")).isEqualTo(exceptionClassName);
        assertThat(((String) getField(record, "exceptionClassName")).length()).isEqualTo(500);
        assertThat((int) getField(record, "retryCount")).isEqualTo(retryCount);
        assertThat((FailedIntegrationEventRecord.Status) getField(record, "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
        Instant createdAt = (Instant) getField(record, "createdAt");
        assertThat(createdAt).isBetween(before, after);
    }

    @Test
    void constructor_allStringFieldsOneCharBelowLimit_accepted() throws Exception {
        String eventClassName = "a".repeat(499);
        String eventPayload = "b".repeat(3999);
        String exceptionMessage = "c".repeat(1999);
        String exceptionClassName = "d".repeat(499);

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                eventClassName, eventPayload, exceptionMessage, exceptionClassName, 3);

        assertThat(((String) getField(record, "eventClassName")).length()).isEqualTo(499);
        assertThat(((String) getField(record, "eventPayload")).length()).isEqualTo(3999);
        assertThat(((String) getField(record, "exceptionMessage")).length()).isEqualTo(1999);
        assertThat(((String) getField(record, "exceptionClassName")).length()).isEqualTo(499);
    }

    @Test
    void constructor_allStringFieldsExceedingLimits_acceptedByConstructor() throws Exception {
        // Constructor does NOT enforce column limits (JPA/DB does). Verify constructor accepts over-limit.
        String eventClassName = "a".repeat(501);
        String eventPayload = "b".repeat(4001);
        String exceptionMessage = "c".repeat(2001);
        String exceptionClassName = "d".repeat(501);

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                eventClassName, eventPayload, exceptionMessage, exceptionClassName, 3);

        assertThat(((String) getField(record, "eventClassName")).length()).isEqualTo(501);
        assertThat(((String) getField(record, "eventPayload")).length()).isEqualTo(4001);
        assertThat(((String) getField(record, "exceptionMessage")).length()).isEqualTo(2001);
        assertThat(((String) getField(record, "exceptionClassName")).length()).isEqualTo(501);
    }

    @Test
    void constructor_mixedNullAndMaxLengthFields_accepted() throws Exception {
        // Nullable fields (exceptionMessage, exceptionClassName) are null while others are at max
        String eventClassName = "x".repeat(500);
        String eventPayload = "y".repeat(4000);

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                eventClassName, eventPayload, null, null, 3);

        assertThat(getField(record, "eventClassName")).isEqualTo(eventClassName);
        assertThat(getField(record, "eventPayload")).isEqualTo(eventPayload);
        assertThat(getField(record, "exceptionMessage")).isNull();
        assertThat(getField(record, "exceptionClassName")).isNull();
        assertThat((FailedIntegrationEventRecord.Status) getField(record, "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void constructor_totalStringBytesExceedTenKB_accepted() throws Exception {
        // Worst case: all fields at limits = 500 + 4000 + 2000 + 500 = 7000 characters
        // With UTF-8 encoding (3 bytes per char for CJK), this could be ~21KB
        String eventClassName = "\u4e16".repeat(500);    // CJK character
        String eventPayload = "\u4e16".repeat(4000);
        String exceptionMessage = "\u4e16".repeat(2000);
        String exceptionClassName = "\u4e16".repeat(500);

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                eventClassName, eventPayload, exceptionMessage, exceptionClassName, 3);

        assertThat(((String) getField(record, "eventClassName")).length()).isEqualTo(500);
        assertThat(((String) getField(record, "eventPayload")).length()).isEqualTo(4000);
        assertThat(((String) getField(record, "exceptionMessage")).length()).isEqualTo(2000);
        assertThat(((String) getField(record, "exceptionClassName")).length()).isEqualTo(500);
    }

    @Test
    void constructor_multipleMaxLengthInstances_areIndependent() throws Exception {
        String eventClassName = "a".repeat(500);
        String eventPayload = "b".repeat(4000);
        String exceptionMessage = "c".repeat(2000);
        String exceptionClassName = "d".repeat(500);

        FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                eventClassName, eventPayload, exceptionMessage, exceptionClassName, 1);
        FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                eventClassName, eventPayload, exceptionMessage, exceptionClassName, 2);

        // Same string content but different retryCount → proves no shared mutable state
        assertThat((int) getField(record1, "retryCount")).isEqualTo(1);
        assertThat((int) getField(record2, "retryCount")).isEqualTo(2);
        assertThat(getField(record1, "eventPayload")).isEqualTo(getField(record2, "eventPayload"));
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
