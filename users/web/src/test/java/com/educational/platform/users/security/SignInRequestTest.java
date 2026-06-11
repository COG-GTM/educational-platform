package com.educational.platform.users.security;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SignInRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void constructor_allFieldsSet_accessorsReturnCorrectValues() {
        // when
        final SignInRequest request = new SignInRequest("user", "pass123");

        // then
        assertThat(request.username()).isEqualTo("user");
        assertThat(request.password()).isEqualTo("pass123");
    }

    @Test
    void validate_validRequest_noViolations() {
        // given
        final SignInRequest request = new SignInRequest("testuser", "testpass");

        // when
        final Set<ConstraintViolation<SignInRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void validate_nullUsername_violation() {
        // given
        final SignInRequest request = new SignInRequest(null, "testpass");

        // when
        final Set<ConstraintViolation<SignInRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void validate_blankUsername_violation() {
        // given
        final SignInRequest request = new SignInRequest("  ", "testpass");

        // when
        final Set<ConstraintViolation<SignInRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void validate_nullPassword_violation() {
        // given
        final SignInRequest request = new SignInRequest("testuser", null);

        // when
        final Set<ConstraintViolation<SignInRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void validate_blankPassword_violation() {
        // given
        final SignInRequest request = new SignInRequest("testuser", "");

        // when
        final Set<ConstraintViolation<SignInRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void validate_bothFieldsNull_multipleViolations() {
        // given
        final SignInRequest request = new SignInRequest(null, null);

        // when
        final Set<ConstraintViolation<SignInRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final SignInRequest request1 = new SignInRequest("user", "pass");
        final SignInRequest request2 = new SignInRequest("user", "pass");

        // then
        assertThat(request1).isEqualTo(request2);
    }

    @Test
    void equality_differentValues_notEqual() {
        // given
        final SignInRequest request1 = new SignInRequest("user1", "pass1");
        final SignInRequest request2 = new SignInRequest("user2", "pass2");

        // then
        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    void hashCode_sameValues_equal() {
        // given
        final SignInRequest request1 = new SignInRequest("user", "pass");
        final SignInRequest request2 = new SignInRequest("user", "pass");

        // then
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    void toString_containsFieldValues() {
        // given
        final SignInRequest request = new SignInRequest("testuser", "testpass");

        // when
        final String str = request.toString();

        // then
        assertThat(str).contains("testuser");
        assertThat(str).contains("testpass");
    }

    @Test
    void hashCode_differentValues_notEqual() {
        // given
        final SignInRequest request1 = new SignInRequest("user1", "pass1");
        final SignInRequest request2 = new SignInRequest("user2", "pass2");

        // then
        assertThat(request1.hashCode()).isNotEqualTo(request2.hashCode());
    }

    @Test
    void equality_nullComparison_notEqual() {
        // given
        final SignInRequest request = new SignInRequest("user", "pass");

        // then
        assertThat(request).isNotEqualTo(null);
    }

    @Test
    void equality_sameUsernameDifferentPassword_notEqual() {
        // given
        final SignInRequest request1 = new SignInRequest("user", "pass1");
        final SignInRequest request2 = new SignInRequest("user", "pass2");

        // then
        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    void equality_differentUsernameSamePassword_notEqual() {
        // given
        final SignInRequest request1 = new SignInRequest("user1", "pass");
        final SignInRequest request2 = new SignInRequest("user2", "pass");

        // then
        assertThat(request1).isNotEqualTo(request2);
    }
}
