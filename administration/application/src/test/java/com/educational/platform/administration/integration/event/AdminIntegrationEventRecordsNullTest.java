package com.educational.platform.administration.integration.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that integration event records handle null UUID without throwing.
 */
public class AdminIntegrationEventRecordsNullTest {

    @Test
    void courseApprovedEvent_nullCourseId_allowed() {
        // when
        final CourseApprovedByAdminIntegrationEvent event =
                new CourseApprovedByAdminIntegrationEvent(null);

        // then
        assertThat(event.courseId()).isNull();
    }

    @Test
    void courseDeclinedEvent_nullCourseId_allowed() {
        // when
        final CourseDeclinedByAdminIntegrationEvent event =
                new CourseDeclinedByAdminIntegrationEvent(null);

        // then
        assertThat(event.courseId()).isNull();
    }

    @Test
    void courseApprovedEvent_nullCourseId_equalToAnotherNull() {
        assertThat(new CourseApprovedByAdminIntegrationEvent(null))
                .isEqualTo(new CourseApprovedByAdminIntegrationEvent(null));
    }

    @Test
    void courseDeclinedEvent_nullCourseId_equalToAnotherNull() {
        assertThat(new CourseDeclinedByAdminIntegrationEvent(null))
                .isEqualTo(new CourseDeclinedByAdminIntegrationEvent(null));
    }

    @Test
    void courseApprovedEvent_hashCode_consistentForNull() {
        final CourseApprovedByAdminIntegrationEvent event =
                new CourseApprovedByAdminIntegrationEvent(null);
        assertThat(event.hashCode()).isEqualTo(event.hashCode());
    }

    @Test
    void courseDeclinedEvent_hashCode_consistentForNull() {
        final CourseDeclinedByAdminIntegrationEvent event =
                new CourseDeclinedByAdminIntegrationEvent(null);
        assertThat(event.hashCode()).isEqualTo(event.hashCode());
    }
}
