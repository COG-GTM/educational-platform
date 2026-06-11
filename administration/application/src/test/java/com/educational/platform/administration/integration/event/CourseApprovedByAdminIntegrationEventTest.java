package com.educational.platform.administration.integration.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseApprovedByAdminIntegrationEventTest {

    @Test
    void constructor_validCourseId_eventCreated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseApprovedByAdminIntegrationEvent sut = new CourseApprovedByAdminIntegrationEvent(courseId);

        // then
        assertThat(sut.courseId()).isEqualTo(courseId);
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseApprovedByAdminIntegrationEvent event1 = new CourseApprovedByAdminIntegrationEvent(courseId);
        final CourseApprovedByAdminIntegrationEvent event2 = new CourseApprovedByAdminIntegrationEvent(courseId);

        // then
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void equality_differentValues_notEqual() {
        // given
        final CourseApprovedByAdminIntegrationEvent event1 = new CourseApprovedByAdminIntegrationEvent(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));
        final CourseApprovedByAdminIntegrationEvent event2 = new CourseApprovedByAdminIntegrationEvent(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // then
        assertThat(event1).isNotEqualTo(event2);
    }
}
