package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentConstructionTest {

    @Test
    void constructor_setsFieldsCorrectly() {
        // given
        final Integer courseId = 10;
        final Integer studentId = 20;

        // when
        final CourseEnrollment enrollment = new CourseEnrollment(courseId, studentId);

        // then
        assertThat(enrollment)
                .hasFieldOrPropertyWithValue("course", courseId)
                .hasFieldOrPropertyWithValue("student", studentId)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
        assertThat(enrollment.getUuid()).isNotNull();
    }

    @Test
    void constructor_twoInstances_generateDistinctUuids() {
        // when
        final CourseEnrollment first = new CourseEnrollment(1, 1);
        final CourseEnrollment second = new CourseEnrollment(1, 1);

        // then
        assertThat(first.getUuid()).isNotEqualTo(second.getUuid());
    }
}
