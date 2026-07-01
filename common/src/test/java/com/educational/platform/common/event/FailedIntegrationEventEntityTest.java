package com.educational.platform.common.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FailedIntegrationEventEntityTest {

    @Test
    void constructor_populatesFieldsAndDefaultsToFailedStatus() {
        // when
        final Instant before = Instant.now();
        final FailedIntegrationEventEntity entity = new FailedIntegrationEventEntity(
                "com.example.SomeEvent", "{\"courseId\":\"abc\"}", "boom", 3);
        final Instant after = Instant.now();

        // then
        assertThat(entity.getEventClassName()).isEqualTo("com.example.SomeEvent");
        assertThat(entity.getEventPayload()).isEqualTo("{\"courseId\":\"abc\"}");
        assertThat(entity.getExceptionMessage()).isEqualTo("boom");
        assertThat(entity.getRetryCount()).isEqualTo(3);
        assertThat(entity.getStatus()).isEqualTo(FailedIntegrationEventStatus.FAILED);
        assertThat(entity.getCreatedAt()).isBetween(before, after);
        assertThat(entity.getId()).isNull();
    }

    @Test
    void constructor_acceptsNullPayloadAndMessage() {
        // when
        final FailedIntegrationEventEntity entity = new FailedIntegrationEventEntity(
                "com.example.SomeEvent", null, null, 0);

        // then
        assertThat(entity.getEventPayload()).isNull();
        assertThat(entity.getExceptionMessage()).isNull();
        assertThat(entity.getRetryCount()).isZero();
        assertThat(entity.getStatus()).isEqualTo(FailedIntegrationEventStatus.FAILED);
    }

    @Test
    void markResolved_transitionsStatusFromFailedToResolved() {
        // given
        final FailedIntegrationEventEntity entity = new FailedIntegrationEventEntity(
                "com.example.SomeEvent", "payload", "boom", 3);
        assertThat(entity.getStatus()).isEqualTo(FailedIntegrationEventStatus.FAILED);

        // when
        entity.markResolved();

        // then
        assertThat(entity.getStatus()).isEqualTo(FailedIntegrationEventStatus.RESOLVED);
    }

    @Test
    void markResolved_isIdempotent() {
        // given
        final FailedIntegrationEventEntity entity = new FailedIntegrationEventEntity(
                "com.example.SomeEvent", "payload", "boom", 3);

        // when
        entity.markResolved();
        entity.markResolved();

        // then
        assertThat(entity.getStatus()).isEqualTo(FailedIntegrationEventStatus.RESOLVED);
    }
}
