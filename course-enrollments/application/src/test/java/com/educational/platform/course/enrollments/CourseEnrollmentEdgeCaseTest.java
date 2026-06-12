package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentEdgeCaseTest {

    @Test
    void complete_calledTwice_remainsCompleted() {
        // given
        final CourseEnrollment sut = new CourseEnrollment(1, 2);

        // when
        sut.complete();
        sut.complete();

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void complete_preservesCourseAndStudent() {
        // given
        final CourseEnrollment sut = new CourseEnrollment(10, 20);

        // when
        sut.complete();

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("course", 10)
                .hasFieldOrPropertyWithValue("student", 20)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void complete_preservesUuid() {
        // given
        final CourseEnrollment sut = new CourseEnrollment(1, 2);
        final java.util.UUID originalUuid = sut.getUuid();

        // when
        sut.complete();

        // then
        assertThat(sut.getUuid()).isEqualTo(originalUuid);
    }
}
