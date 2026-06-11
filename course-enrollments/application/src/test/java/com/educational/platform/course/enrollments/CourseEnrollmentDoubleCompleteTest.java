package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests idempotent behavior of {@link CourseEnrollment#complete()} and
 * initial construction state.
 */
public class CourseEnrollmentDoubleCompleteTest {

    @Test
    void constructor_setsInProgressStatus() {
        // when
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

        // then
        assertThat(enrollment)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
    }

    @Test
    void constructor_generatesUuid() {
        // when
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

        // then
        assertThat(enrollment.getUuid()).isNotNull();
    }

    @Test
    void constructor_storesCourseAndStudent() {
        // when
        final CourseEnrollment enrollment = new CourseEnrollment(10, 20);

        // then
        assertThat(enrollment)
                .hasFieldOrPropertyWithValue("course", 10)
                .hasFieldOrPropertyWithValue("student", 20);
    }

    @Test
    void complete_setsCompletedStatus() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

        // when
        enrollment.complete();

        // then
        assertThat(enrollment)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void complete_calledTwice_remainsCompleted() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        enrollment.complete();

        // when
        enrollment.complete();

        // then
        assertThat(enrollment)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void twoEnrollments_haveDifferentUuids() {
        // when
        final CourseEnrollment first = new CourseEnrollment(1, 1);
        final CourseEnrollment second = new CourseEnrollment(1, 1);

        // then
        assertThat(first.getUuid()).isNotEqualTo(second.getUuid());
    }
}
