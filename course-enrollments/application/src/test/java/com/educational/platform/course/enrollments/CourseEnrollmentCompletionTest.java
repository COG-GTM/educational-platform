package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentCompletionTest {

    @Test
    void complete_fromInProgress_transitionsToCompleted() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);

        // when
        enrollment.complete();

        // then
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void complete_calledTwice_remainsCompleted() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        enrollment.complete();

        // when
        enrollment.complete();

        // then
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void complete_preservesUuid() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        final java.util.UUID originalUuid = enrollment.getUuid();

        // when
        enrollment.complete();

        // then
        assertThat(enrollment.getUuid()).isEqualTo(originalUuid);
    }

    @Test
    void complete_preservesCourseAndStudentReferences() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(42, 99);

        // when
        enrollment.complete();

        // then
        assertThat(enrollment)
                .hasFieldOrPropertyWithValue("course", 42)
                .hasFieldOrPropertyWithValue("student", 99);
    }
}
