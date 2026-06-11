package com.educational.platform.users.security;

import com.educational.platform.users.RoleDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the Bean Validation constraints on {@link SignUpRequest}.
 * Constraints: role is {@code @NotNull}, username/email/password are {@code @NotBlank}.
 */
public class SignUpRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequest_noViolations() {
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "password123");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void nullRole_violation() {
        final SignUpRequest request = new SignUpRequest(null, "user", "user@example.com", "password");
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("role"));
    }

    @Test
    void nullUsername_violation() {
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_TEACHER, null, "e@e.com", "pass");
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void blankEmail_violation() {
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "  ", "pass");
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void nullPassword_violation() {
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "e@e.com", null);
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void allFieldsNull_fourViolations() {
        final SignUpRequest request = new SignUpRequest(null, null, null, null);
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(4);
    }

    @Test
    void teacherRole_valid() {
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_TEACHER, "teacher1", "t@t.com", "securePass");
        assertThat(validator.validate(request)).isEmpty();
    }
}
