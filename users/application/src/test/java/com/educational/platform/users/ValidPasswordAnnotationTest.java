package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying that @ValidPassword annotation on UserRegistrationCommand
 * triggers PasswordConstraintValidator via the Bean Validation framework.
 */
public class ValidPasswordAnnotationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validPassword_noPasswordViolation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password("validpass")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "abc", "1234567"})
    void tooShortPassword_passwordViolation(String password) {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password(password)
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void passwordWithWhitespace_passwordViolation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password("pass word1")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void passwordExceedsMaxLength_passwordViolation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password("a".repeat(31))
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void passwordExactMinLength_noPasswordViolation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password("a".repeat(8))
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void passwordExactMaxLength_noPasswordViolation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password("a".repeat(30))
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void nullPassword_notBlankViolationNotPasswordViolation() {
        // given — @ValidPassword allows null, but @NotBlank rejects it
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password(null)
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        // all violations on password should be from @NotBlank, not from @ValidPassword
        violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("password"))
                .forEach(v -> assertThat(v.getMessage()).isNotEqualTo("Invalid Password"));
    }

    @Test
    void passwordWithTabCharacter_passwordViolation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password("pass\tword1")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }
}
