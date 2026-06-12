package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourseEnrollmentEdgeCaseTest {

    @Test
    void constructor_validArguments_initialStatusInProgress() {
        // when
        final CourseEnrollment sut = new CourseEnrollment(1, 2);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS)
                .hasFieldOrPropertyWithValue("course", 1)
                .hasFieldOrPropertyWithValue("student", 2);
    }

    @Test
    void constructor_validArguments_uuidGenerated() {
        // when
        final CourseEnrollment sut = new CourseEnrollment(1, 2);

        // then
        assertThat(sut.getUuid()).isNotNull();
    }

    @Test
    void constructor_twoCalls_differentUuids() {
        // when
        final CourseEnrollment enrollment1 = new CourseEnrollment(1, 2);
        final CourseEnrollment enrollment2 = new CourseEnrollment(1, 2);

        // then
        assertThat(enrollment1.getUuid()).isNotEqualTo(enrollment2.getUuid());
    }

    @Test
    void complete_inProgressEnrollment_statusChangedToCompleted() {
        // given
        final CourseEnrollment sut = new CourseEnrollment(1, 2);

        // when
        sut.complete();

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void complete_calledTwice_remainsCompleted() {
        // given
        final CourseEnrollment sut = new CourseEnrollment(1, 2);

        // when
        sut.complete();
        sut.complete();

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
    }

    @Test
    void getUuid_afterCreation_consistentValue() {
        // given
        final CourseEnrollment sut = new CourseEnrollment(1, 2);

        // when
        final var uuid1 = sut.getUuid();
        final var uuid2 = sut.getUuid();

        // then
        assertThat(uuid1).isEqualTo(uuid2);
    }
}
