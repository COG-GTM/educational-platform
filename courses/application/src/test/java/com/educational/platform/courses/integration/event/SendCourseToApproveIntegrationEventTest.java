package com.educational.platform.courses.integration.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SendCourseToApproveIntegrationEventTest {

    @Test
    void constructor_validCourseId_eventCreated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final SendCourseToApproveIntegrationEvent sut = new SendCourseToApproveIntegrationEvent(courseId);

        // then
        assertThat(sut.courseId()).isEqualTo(courseId);
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final SendCourseToApproveIntegrationEvent event1 = new SendCourseToApproveIntegrationEvent(courseId);
        final SendCourseToApproveIntegrationEvent event2 = new SendCourseToApproveIntegrationEvent(courseId);

        // then
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void equality_differentValues_notEqual() {
        // given
        final SendCourseToApproveIntegrationEvent event1 = new SendCourseToApproveIntegrationEvent(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));
        final SendCourseToApproveIntegrationEvent event2 = new SendCourseToApproveIntegrationEvent(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // then
        assertThat(event1).isNotEqualTo(event2);
    }
}
