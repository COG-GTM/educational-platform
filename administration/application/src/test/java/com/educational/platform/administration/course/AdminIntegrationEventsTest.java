package com.educational.platform.administration.course;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.administration.integration.event.CourseDeclinedByAdminIntegrationEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminIntegrationEventsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void courseApprovedByAdminIntegrationEvent_exposesCourseId() {
        // when
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(UUID_VALUE);

        // then
        assertThat(event.courseId()).isEqualTo(UUID_VALUE);
    }

    @Test
    void courseApprovedByAdminIntegrationEvent_equalInstances() {
        assertThat(new CourseApprovedByAdminIntegrationEvent(UUID_VALUE))
                .isEqualTo(new CourseApprovedByAdminIntegrationEvent(UUID_VALUE));
    }

    @Test
    void courseApprovedByAdminIntegrationEvent_differentUuids_notEqual() {
        assertThat(new CourseApprovedByAdminIntegrationEvent(UUID_VALUE))
                .isNotEqualTo(new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID()));
    }

    @Test
    void courseDeclinedByAdminIntegrationEvent_exposesCourseId() {
        // when
        final CourseDeclinedByAdminIntegrationEvent event = new CourseDeclinedByAdminIntegrationEvent(UUID_VALUE);

        // then
        assertThat(event.courseId()).isEqualTo(UUID_VALUE);
    }

    @Test
    void courseDeclinedByAdminIntegrationEvent_equalInstances() {
        assertThat(new CourseDeclinedByAdminIntegrationEvent(UUID_VALUE))
                .isEqualTo(new CourseDeclinedByAdminIntegrationEvent(UUID_VALUE));
    }

    @Test
    void courseDeclinedByAdminIntegrationEvent_differentUuids_notEqual() {
        assertThat(new CourseDeclinedByAdminIntegrationEvent(UUID_VALUE))
                .isNotEqualTo(new CourseDeclinedByAdminIntegrationEvent(UUID.randomUUID()));
    }
}
