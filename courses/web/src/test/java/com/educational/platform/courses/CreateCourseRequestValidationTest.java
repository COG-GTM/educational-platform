package com.educational.platform.courses;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the Bean Validation constraints on {@link CreateCourseRequest}.
 */
public class CreateCourseRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequest_noViolations() {
        final CreateCourseRequest request = new CreateCourseRequest("Math 101", "Intro to math");
        final Set<ConstraintViolation<CreateCourseRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void nullName_violation() {
        final CreateCourseRequest request = new CreateCourseRequest(null, "description");
        final Set<ConstraintViolation<CreateCourseRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void blankName_violation() {
        final CreateCourseRequest request = new CreateCourseRequest("  ", "description");
        final Set<ConstraintViolation<CreateCourseRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void nullDescription_violation() {
        final CreateCourseRequest request = new CreateCourseRequest("name", null);
        final Set<ConstraintViolation<CreateCourseRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    void blankDescription_violation() {
        final CreateCourseRequest request = new CreateCourseRequest("name", "");
        final Set<ConstraintViolation<CreateCourseRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    void bothFieldsBlank_twoViolations() {
        final CreateCourseRequest request = new CreateCourseRequest("", "");
        final Set<ConstraintViolation<CreateCourseRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(2);
    }
}
