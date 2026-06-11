package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link CourseEnrollment} constructor and initial state.
 */
public class CourseEnrollmentConstructorTest {

    @Test
    void constructor_setsFieldsCorrectly() {
        // when
        final CourseEnrollment enrollment = new CourseEnrollment(10, 20);

        // then
        assertThat(enrollment.getUuid()).isNotNull();
        assertThat(enrollment).hasFieldOrPropertyWithValue("course", 10);
        assertThat(enrollment).hasFieldOrPropertyWithValue("student", 20);
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
    }

    @Test
    void constructor_twoEnrollments_haveDifferentUuids() {
        // when
        final CourseEnrollment enrollment1 = new CourseEnrollment(1, 1);
        final CourseEnrollment enrollment2 = new CourseEnrollment(1, 1);

        // then
        assertThat(enrollment1.getUuid()).isNotEqualTo(enrollment2.getUuid());
    }

    @Test
    void complete_changesStatusToCompleted() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 1);

        // when
        enrollment.complete();

        // then
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void complete_idempotent_calledTwice_remainsCompleted() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 1);

        // when
        enrollment.complete();
        enrollment.complete();

        // then
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void getUuid_afterComplete_preservesOriginalUuid() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 1);
        final var originalUuid = enrollment.getUuid();

        // when
        enrollment.complete();

        // then
        assertThat(enrollment.getUuid()).isEqualTo(originalUuid);
    }
}
