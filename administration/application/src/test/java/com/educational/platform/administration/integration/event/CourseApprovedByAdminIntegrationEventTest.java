package com.educational.platform.administration.integration.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseApprovedByAdminIntegrationEventTest {

    @Test
    void courseId_preservesUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);

        // then
        assertThat(event.courseId()).isEqualTo(uuid);
    }

    @Test
    void courseId_nullUuid_preservesNull() {
        // when
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(null);

        // then
        assertThat(event.courseId()).isNull();
    }

    @Test
    void recordEquality_sameUuid_equal() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseApprovedByAdminIntegrationEvent event1 = new CourseApprovedByAdminIntegrationEvent(uuid);
        final CourseApprovedByAdminIntegrationEvent event2 = new CourseApprovedByAdminIntegrationEvent(uuid);

        // then
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void recordEquality_differentUuid_notEqual() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseApprovedByAdminIntegrationEvent event1 = new CourseApprovedByAdminIntegrationEvent(uuid1);
        final CourseApprovedByAdminIntegrationEvent event2 = new CourseApprovedByAdminIntegrationEvent(uuid2);

        // then
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    void toString_containsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);

        // then
        assertThat(event.toString()).contains(uuid.toString());
    }
}
