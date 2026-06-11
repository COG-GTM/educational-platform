package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentRequestTest {

    @Test
    void constructor_validStudent_requestCreated() {
        // when
        final CourseEnrollmentRequest sut = new CourseEnrollmentRequest("student-username");

        // then
        assertThat(sut.student()).isEqualTo("student-username");
    }

    @Test
    void constructor_nullStudent_requestCreated() {
        // when
        final CourseEnrollmentRequest sut = new CourseEnrollmentRequest(null);

        // then
        assertThat(sut.student()).isNull();
    }

    @Test
    void equals_sameValues_equal() {
        // given
        final CourseEnrollmentRequest request1 = new CourseEnrollmentRequest("student");
        final CourseEnrollmentRequest request2 = new CourseEnrollmentRequest("student");

        // when / then
        assertThat(request1).isEqualTo(request2);
    }
}
