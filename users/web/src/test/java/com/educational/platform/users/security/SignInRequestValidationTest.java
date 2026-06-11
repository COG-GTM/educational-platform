package com.educational.platform.users.security;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the Bean Validation constraints on {@link SignInRequest}.
 * Both username and password are {@code @NotBlank}.
 */
public class SignInRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequest_noViolations() {
        assertThat(validator.validate(new SignInRequest("user", "pass123"))).isEmpty();
    }

    @Test
    void nullUsername_violation() {
        final Set<ConstraintViolation<SignInRequest>> violations =
                validator.validate(new SignInRequest(null, "pass"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void blankUsername_violation() {
        final Set<ConstraintViolation<SignInRequest>> violations =
                validator.validate(new SignInRequest("  ", "pass"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void nullPassword_violation() {
        final Set<ConstraintViolation<SignInRequest>> violations =
                validator.validate(new SignInRequest("user", null));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void blankPassword_violation() {
        final Set<ConstraintViolation<SignInRequest>> violations =
                validator.validate(new SignInRequest("user", ""));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void bothFieldsNull_twoViolations() {
        final Set<ConstraintViolation<SignInRequest>> violations =
                validator.validate(new SignInRequest(null, null));
        assertThat(violations).hasSize(2);
    }
}
