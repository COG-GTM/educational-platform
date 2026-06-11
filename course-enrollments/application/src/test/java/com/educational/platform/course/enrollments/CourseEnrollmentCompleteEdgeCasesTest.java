package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests edge cases for {@link CourseEnrollment#complete()}: double-complete
 * idempotency and initial state verification.
 */
public class CourseEnrollmentCompleteEdgeCasesTest {

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
    void constructor_setsUniqueUuids() {
        // when
        final CourseEnrollment first = new CourseEnrollment(1, 2);
        final CourseEnrollment second = new CourseEnrollment(1, 2);

        // then
        assertThat(first.getUuid()).isNotNull();
        assertThat(second.getUuid()).isNotNull();
        assertThat(first.getUuid()).isNotEqualTo(second.getUuid());
    }
}
