package com.educational.platform.common.event;

import org.junit.jupiter.api.Test;

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

}
