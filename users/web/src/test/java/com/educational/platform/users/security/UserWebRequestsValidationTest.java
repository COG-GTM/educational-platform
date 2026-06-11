package com.educational.platform.users.security;

import com.educational.platform.users.RoleDTO;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates {@code @NotBlank} / {@code @NotNull} constraints on SignInRequest and SignUpRequest.
 */
public class UserWebRequestsValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    // --- SignInRequest ---

    @Test
    void signInRequest_validValues_noViolations() {
        final SignInRequest request = new SignInRequest("user", "pass");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void signInRequest_blankUsername_hasViolation() {
        final SignInRequest request = new SignInRequest("", "pass");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void signInRequest_nullUsername_hasViolation() {
        final SignInRequest request = new SignInRequest(null, "pass");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void signInRequest_blankPassword_hasViolation() {
        final SignInRequest request = new SignInRequest("user", "");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void signInRequest_nullPassword_hasViolation() {
        final SignInRequest request = new SignInRequest("user", null);
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void signInRequest_bothNull_hasTwoViolations() {
        final SignInRequest request = new SignInRequest(null, null);
        assertThat(validator.validate(request)).hasSize(2);
    }

    // --- SignUpRequest ---

    @Test
    void signUpRequest_validValues_noViolations() {
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "email@test.com", "password");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void signUpRequest_nullRole_hasViolation() {
        final SignUpRequest request = new SignUpRequest(null, "user", "email@test.com", "password");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void signUpRequest_blankUsername_hasViolation() {
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "", "email@test.com", "password");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void signUpRequest_nullUsername_hasViolation() {
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, null, "email@test.com", "password");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void signUpRequest_blankEmail_hasViolation() {
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "", "password");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void signUpRequest_nullEmail_hasViolation() {
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", null, "password");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void signUpRequest_blankPassword_hasViolation() {
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "email@test.com", "");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void signUpRequest_nullPassword_hasViolation() {
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "email@test.com", null);
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void signUpRequest_allNulls_hasMultipleViolations() {
        final SignUpRequest request = new SignUpRequest(null, null, null, null);
        assertThat(validator.validate(request)).hasSizeGreaterThanOrEqualTo(4);
    }
}
