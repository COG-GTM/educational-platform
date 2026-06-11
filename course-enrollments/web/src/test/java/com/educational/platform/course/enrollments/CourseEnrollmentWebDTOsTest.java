package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentWebDTOsTest {

    @Test
    void courseEnrollmentRequest_exposesStudent() {
        // when
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest("student-name");

        // then
        assertThat(request.student()).isEqualTo("student-name");
    }

    @Test
    void courseEnrollmentRequest_equalInstances() {
        assertThat(new CourseEnrollmentRequest("student"))
                .isEqualTo(new CourseEnrollmentRequest("student"));
    }

    @Test
    void courseEnrollmentRequest_differentStudent_notEqual() {
        assertThat(new CourseEnrollmentRequest("student1"))
                .isNotEqualTo(new CourseEnrollmentRequest("student2"));
    }
}
