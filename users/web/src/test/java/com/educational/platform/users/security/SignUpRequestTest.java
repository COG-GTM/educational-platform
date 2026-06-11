package com.educational.platform.users.security;

import com.educational.platform.users.RoleDTO;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SignUpRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void constructor_allFieldsSet_accessorsReturnCorrectValues() {
        // when
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "password1");

        // then
        assertThat(request.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
        assertThat(request.username()).isEqualTo("user");
        assertThat(request.email()).isEqualTo("user@example.com");
        assertThat(request.password()).isEqualTo("password1");
    }

    @Test
    void validate_validStudentRequest_noViolations() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "testuser", "test@example.com", "password1");

        // when
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void validate_validTeacherRequest_noViolations() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_TEACHER, "teacher", "teacher@example.com", "password1");

        // when
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void validate_nullRole_violation() {
        // given
        final SignUpRequest request = new SignUpRequest(null, "testuser", "test@example.com", "password1");

        // when
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("role"));
    }

    @Test
    void validate_nullUsername_violation() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, null, "test@example.com", "password1");

        // when
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void validate_blankUsername_violation() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "  ", "test@example.com", "password1");

        // when
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void validate_nullEmail_violation() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "testuser", null, "password1");

        // when
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void validate_blankEmail_violation() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "testuser", "", "password1");

        // when
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void validate_nullPassword_violation() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "testuser", "test@example.com", null);

        // when
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void validate_blankPassword_violation() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "testuser", "test@example.com", "");

        // when
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void validate_allFieldsNull_multipleViolations() {
        // given
        final SignUpRequest request = new SignUpRequest(null, null, null, null);

        // when
        final Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final SignUpRequest request1 = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "pass");
        final SignUpRequest request2 = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "pass");

        // then
        assertThat(request1).isEqualTo(request2);
    }

    @Test
    void equality_differentValues_notEqual() {
        // given
        final SignUpRequest request1 = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user1", "user1@example.com", "pass1");
        final SignUpRequest request2 = new SignUpRequest(RoleDTO.ROLE_TEACHER, "user2", "user2@example.com", "pass2");

        // then
        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    void hashCode_sameValues_equal() {
        // given
        final SignUpRequest request1 = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "pass");
        final SignUpRequest request2 = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "pass");

        // then
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    void toString_containsFieldValues() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "testuser", "test@example.com", "pass123");

        // when
        final String str = request.toString();

        // then
        assertThat(str).contains("testuser");
        assertThat(str).contains("test@example.com");
        assertThat(str).contains("ROLE_STUDENT");
    }
}
