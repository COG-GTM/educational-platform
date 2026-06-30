package com.educational.platform.common.event;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FailedIntegrationEventTest {

    @Test
    void of_populatesAllFieldsAndDefaultsToFailedStatus() {
        // given
        final Instant before = Instant.now();

        // when
        final FailedIntegrationEvent event = FailedIntegrationEvent.of(
                "com.educational.platform.SomeEvent", "{\"id\":1}", "boom", 3);

        // then
        final Instant after = Instant.now();
        assertThat(event.getEventClassName()).isEqualTo("com.educational.platform.SomeEvent");
        assertThat(event.getEventPayload()).isEqualTo("{\"id\":1}");
        assertThat(event.getExceptionMessage()).isEqualTo("boom");
        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getStatus()).isEqualTo(FailedEventStatus.FAILED);
        assertThat(event.getTimestamp()).isBetween(before, after);
        assertThat(event.getId()).isNull();
    }

    @Test
    void resolve_transitionsStatusFromFailedToResolved() {
        // given
        final FailedIntegrationEvent event = FailedIntegrationEvent.of("Event", "{}", "msg", 1);
        assertThat(event.getStatus()).isEqualTo(FailedEventStatus.FAILED);

        // when
        event.resolve();

        // then
        assertThat(event.getStatus()).isEqualTo(FailedEventStatus.RESOLVED);
    }

    @Test
    void resolve_isIdempotentWhenAlreadyResolved() {
        // given
        final FailedIntegrationEvent event = FailedIntegrationEvent.of("Event", "{}", "msg", 1);
        event.resolve();

        // when
        event.resolve();

        // then
        assertThat(event.getStatus()).isEqualTo(FailedEventStatus.RESOLVED);
    }

    @Test
    void of_acceptsNullPayloadAndExceptionMessage() {
        // when
        final FailedIntegrationEvent event = FailedIntegrationEvent.of("Event", null, null, 0);

        // then
        assertThat(event.getEventPayload()).isNull();
        assertThat(event.getExceptionMessage()).isNull();
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getStatus()).isEqualTo(FailedEventStatus.FAILED);
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void of_acceptsZeroAndNegativeRetryCountBoundaries() {
        // expect
        assertThat(FailedIntegrationEvent.of("Event", "{}", "msg", 0).getRetryCount()).isZero();
        assertThat(FailedIntegrationEvent.of("Event", "{}", "msg", -1).getRetryCount()).isEqualTo(-1);
        assertThat(FailedIntegrationEvent.of("Event", "{}", "msg", Integer.MAX_VALUE).getRetryCount())
                .isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void of_createsIndependentInstancesPerInvocation() {
        // when
        final FailedIntegrationEvent first = FailedIntegrationEvent.of("EventA", "{}", "a", 1);
        final FailedIntegrationEvent second = FailedIntegrationEvent.of("EventB", "{}", "b", 2);

        // then
        assertThat(first).isNotSameAs(second);
        assertThat(first.getEventClassName()).isEqualTo("EventA");
        assertThat(second.getEventClassName()).isEqualTo("EventB");
        first.resolve();
        assertThat(second.getStatus()).isEqualTo(FailedEventStatus.FAILED);
    }
}
