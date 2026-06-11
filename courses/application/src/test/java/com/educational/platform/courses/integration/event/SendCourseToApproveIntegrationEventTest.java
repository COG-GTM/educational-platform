package com.educational.platform.courses.integration.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link SendCourseToApproveIntegrationEvent} record accessors and equality.
 */
public class SendCourseToApproveIntegrationEventTest {

    @Test
    void courseId_returnsProvidedUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);

        // then
        assertThat(event.courseId()).isEqualTo(uuid);
    }

    @Test
    void equals_sameUuid_areEqual() {
        final UUID uuid = UUID.randomUUID();
        assertThat(new SendCourseToApproveIntegrationEvent(uuid))
                .isEqualTo(new SendCourseToApproveIntegrationEvent(uuid));
    }

    @Test
    void equals_differentUuid_areNotEqual() {
        assertThat(new SendCourseToApproveIntegrationEvent(UUID.randomUUID()))
                .isNotEqualTo(new SendCourseToApproveIntegrationEvent(UUID.randomUUID()));
    }
}
