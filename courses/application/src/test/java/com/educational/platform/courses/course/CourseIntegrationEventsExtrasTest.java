package com.educational.platform.courses.course;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseIntegrationEventsExtrasTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveIntegrationEvent_exposesCourseId() {
        // when
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(UUID_VALUE);

        // then
        assertThat(event.courseId()).isEqualTo(UUID_VALUE);
    }

    @Test
    void sendCourseToApproveIntegrationEvent_equalInstances() {
        assertThat(new SendCourseToApproveIntegrationEvent(UUID_VALUE))
                .isEqualTo(new SendCourseToApproveIntegrationEvent(UUID_VALUE));
    }

    @Test
    void sendCourseToApproveIntegrationEvent_differentCourseId_notEqual() {
        assertThat(new SendCourseToApproveIntegrationEvent(UUID_VALUE))
                .isNotEqualTo(new SendCourseToApproveIntegrationEvent(UUID.randomUUID()));
    }
}
