package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link CourseEnrollment#getUuid()} returns a stable UUID
 * across multiple invocations and that the UUID is a valid v4 UUID.
 */
public class CourseEnrollmentUuidStabilityTest {

    @Test
    void getUuid_calledMultipleTimes_returnsSameValue() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

        // when
        final UUID first = enrollment.getUuid();
        final UUID second = enrollment.getUuid();
        final UUID third = enrollment.getUuid();

        // then
        assertThat(first).isEqualTo(second).isEqualTo(third);
    }

    @Test
    void getUuid_returnsValidVersion4Uuid() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(10, 20);

        // when
        final UUID uuid = enrollment.getUuid();

        // then
        assertThat(uuid).isNotNull();
        assertThat(uuid.version()).isEqualTo(4);
    }

    @Test
    void getUuid_differentEnrollments_returnDifferentUuids() {
        // given
        final CourseEnrollment first = new CourseEnrollment(1, 2);
        final CourseEnrollment second = new CourseEnrollment(1, 2);

        // then
        assertThat(first.getUuid()).isNotEqualTo(second.getUuid());
    }
}
