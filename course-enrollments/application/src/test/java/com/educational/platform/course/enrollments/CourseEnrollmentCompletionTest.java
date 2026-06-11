package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentCompletionTest {

    @Test
    void complete_inProgressEnrollment_transitionsToCompleted() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

        // when
        enrollment.complete();

        // then
        assertThat(enrollment)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void complete_alreadyCompleted_remainsCompleted() {
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
    void newEnrollment_initialStateIsInProgress() {
        // given / when
        final CourseEnrollment enrollment = new CourseEnrollment(10, 20);

        // then
        assertThat(enrollment)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS)
                .hasFieldOrPropertyWithValue("course", 10)
                .hasFieldOrPropertyWithValue("student", 20);
    }

    @Test
    void getUuid_returnsNonNullUuid() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

        // when / then
        assertThat(enrollment.getUuid()).isNotNull();
    }

    @Test
    void getUuid_distinctEnrollmentsHaveDistinctUuids() {
        // given
        final CourseEnrollment first = new CourseEnrollment(1, 2);
        final CourseEnrollment second = new CourseEnrollment(1, 2);

        // when / then
        assertThat(first.getUuid()).isNotEqualTo(second.getUuid());
    }
}
