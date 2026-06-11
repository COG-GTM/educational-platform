package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentLifecycleTest {

    @Test
    void complete_afterCreation_transitionsToCompleted() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);

        // when
        enrollment.complete();

        // then
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void complete_calledMultipleTimes_remainsCompleted() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

        // when
        enrollment.complete();
        enrollment.complete();

        // then
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void getUuid_afterCreation_isConsistentAcrossMultipleCalls() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

        // then
        assertThat(enrollment.getUuid()).isEqualTo(enrollment.getUuid());
    }
}
