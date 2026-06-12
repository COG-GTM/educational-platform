package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourseEnrollmentCompletionIdempotentTest {

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
    void complete_afterConstruction_statusChangedToCompleted() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(10, 20);
        assertThat(enrollment)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);

        // when
        enrollment.complete();

        // then
        assertThat(enrollment)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void getUuid_afterComplete_uuidPreserved() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        final java.util.UUID originalUuid = enrollment.getUuid();

        // when
        enrollment.complete();

        // then
        assertThat(enrollment.getUuid()).isEqualTo(originalUuid);
    }
}
