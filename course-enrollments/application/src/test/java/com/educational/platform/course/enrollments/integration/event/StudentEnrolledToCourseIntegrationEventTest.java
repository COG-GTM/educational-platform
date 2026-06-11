package com.educational.platform.course.enrollments.integration.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link StudentEnrolledToCourseIntegrationEvent} record accessors and equality.
 */
public class StudentEnrolledToCourseIntegrationEventTest {

    @Test
    void accessors_returnProvidedValues() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final StudentEnrolledToCourseIntegrationEvent event =
                new StudentEnrolledToCourseIntegrationEvent(courseId, "student1");

        // then
        assertThat(event.courseId()).isEqualTo(courseId);
        assertThat(event.username()).isEqualTo("student1");
    }

    @Test
    void equals_sameValues_areEqual() {
        final UUID courseId = UUID.randomUUID();
        assertThat(new StudentEnrolledToCourseIntegrationEvent(courseId, "user"))
                .isEqualTo(new StudentEnrolledToCourseIntegrationEvent(courseId, "user"));
    }

    @Test
    void equals_differentCourseId_areNotEqual() {
        assertThat(new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "user"))
                .isNotEqualTo(new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "user"));
    }

    @Test
    void equals_differentUsername_areNotEqual() {
        final UUID courseId = UUID.randomUUID();
        assertThat(new StudentEnrolledToCourseIntegrationEvent(courseId, "user1"))
                .isNotEqualTo(new StudentEnrolledToCourseIntegrationEvent(courseId, "user2"));
    }
}
