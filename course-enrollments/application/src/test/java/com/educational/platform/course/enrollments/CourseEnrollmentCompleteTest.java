package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link CourseEnrollment#complete()} transitions the completion
 * status from IN_PROGRESS to COMPLETED on the first invocation.
 */
public class CourseEnrollmentCompleteTest {

    @Test
    void complete_fromInProgress_setsStatusToCompleted() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);

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
    void complete_preservesCourseAndStudent() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(10, 20);

        // when
        enrollment.complete();

        // then
        assertThat(enrollment)
                .hasFieldOrPropertyWithValue("course", 10)
                .hasFieldOrPropertyWithValue("student", 20);
    }
}
