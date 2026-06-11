package com.educational.platform.administration.integration.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CourseApprovedByAdminIntegrationEvent} and
 * {@link CourseDeclinedByAdminIntegrationEvent} record accessors and equality.
 */
public class AdminIntegrationEventRecordsTest {

    @Test
    void courseApprovedEvent_courseId_returnsProvidedUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);

        // then
        assertThat(event.courseId()).isEqualTo(uuid);
    }

    @Test
    void courseApprovedEvent_equals_sameUuid_areEqual() {
        final UUID uuid = UUID.randomUUID();
        assertThat(new CourseApprovedByAdminIntegrationEvent(uuid))
                .isEqualTo(new CourseApprovedByAdminIntegrationEvent(uuid));
    }

    @Test
    void courseApprovedEvent_equals_differentUuid_areNotEqual() {
        assertThat(new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID()))
                .isNotEqualTo(new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID()));
    }

    @Test
    void courseDeclinedEvent_courseId_returnsProvidedUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseDeclinedByAdminIntegrationEvent event = new CourseDeclinedByAdminIntegrationEvent(uuid);

        // then
        assertThat(event.courseId()).isEqualTo(uuid);
    }

    @Test
    void courseDeclinedEvent_equals_sameUuid_areEqual() {
        final UUID uuid = UUID.randomUUID();
        assertThat(new CourseDeclinedByAdminIntegrationEvent(uuid))
                .isEqualTo(new CourseDeclinedByAdminIntegrationEvent(uuid));
    }

    @Test
    void courseDeclinedEvent_equals_differentUuid_areNotEqual() {
        assertThat(new CourseDeclinedByAdminIntegrationEvent(UUID.randomUUID()))
                .isNotEqualTo(new CourseDeclinedByAdminIntegrationEvent(UUID.randomUUID()));
    }
}
