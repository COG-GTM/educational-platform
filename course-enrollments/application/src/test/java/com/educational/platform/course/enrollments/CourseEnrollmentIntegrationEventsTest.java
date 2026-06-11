package com.educational.platform.course.enrollments;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentIntegrationEventsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void studentEnrolledToCourseIntegrationEvent_exposesCourseIdAndUsername() {
        // when
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(UUID_VALUE, "student");

        // then
        assertThat(event.courseId()).isEqualTo(UUID_VALUE);
        assertThat(event.username()).isEqualTo("student");
    }

    @Test
    void studentEnrolledToCourseIntegrationEvent_equalInstances() {
        assertThat(new StudentEnrolledToCourseIntegrationEvent(UUID_VALUE, "student"))
                .isEqualTo(new StudentEnrolledToCourseIntegrationEvent(UUID_VALUE, "student"));
    }

    @Test
    void studentEnrolledToCourseIntegrationEvent_differentUsername_notEqual() {
        assertThat(new StudentEnrolledToCourseIntegrationEvent(UUID_VALUE, "student1"))
                .isNotEqualTo(new StudentEnrolledToCourseIntegrationEvent(UUID_VALUE, "student2"));
    }
}
