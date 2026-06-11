package com.educational.platform.course.enrollments;

import com.educational.platform.course.enrollments.query.CourseEnrollmentByUUIDQuery;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQuery;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests query records in the course-enrollments module.
 */
public class EnrollmentQueryCommandsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    // --- CourseEnrollmentByUUIDQuery ---

    @Test
    void courseEnrollmentByUUIDQuery_exposesUuid() {
        final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(UUID_VALUE);
        assertThat(query.uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void courseEnrollmentByUUIDQuery_equalInstances() {
        assertThat(new CourseEnrollmentByUUIDQuery(UUID_VALUE))
                .isEqualTo(new CourseEnrollmentByUUIDQuery(UUID_VALUE));
    }

    @Test
    void courseEnrollmentByUUIDQuery_differentUuids_notEqual() {
        assertThat(new CourseEnrollmentByUUIDQuery(UUID_VALUE))
                .isNotEqualTo(new CourseEnrollmentByUUIDQuery(UUID.randomUUID()));
    }

    @Test
    void courseEnrollmentByUUIDQuery_hashCodeConsistentWithEquals() {
        final CourseEnrollmentByUUIDQuery first = new CourseEnrollmentByUUIDQuery(UUID_VALUE);
        final CourseEnrollmentByUUIDQuery second = new CourseEnrollmentByUUIDQuery(UUID_VALUE);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    // --- ListCourseEnrollmentsQuery ---

    @Test
    void listCourseEnrollmentsQuery_canBeInstantiated() {
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();
        assertThat(query).isNotNull();
    }

    @Test
    void listCourseEnrollmentsQuery_equalInstances() {
        assertThat(new ListCourseEnrollmentsQuery())
                .isEqualTo(new ListCourseEnrollmentsQuery());
    }
}
