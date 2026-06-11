package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validStudent_noViolations() {
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest("student1");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void nullStudent_hasViolation() {
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest(null);
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void accessors() {
        final CourseEnrollmentRequest request = new CourseEnrollmentRequest("john");
        assertThat(request.student()).isEqualTo("john");
    }

    @Test
    void equalInstances() {
        assertThat(new CourseEnrollmentRequest("student"))
                .isEqualTo(new CourseEnrollmentRequest("student"));
    }

    @Test
    void differentStudents_notEqual() {
        assertThat(new CourseEnrollmentRequest("student1"))
                .isNotEqualTo(new CourseEnrollmentRequest("student2"));
    }
}
