package com.educational.platform.users.login;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SignInCommandTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void builder_allFieldsSet_commandCreatedCorrectly() {
        // when
        final SignInCommand command = SignInCommand.builder()
                .username("testuser")
                .password("testpass")
                .build();

        // then
        assertThat(command.username()).isEqualTo("testuser");
        assertThat(command.password()).isEqualTo("testpass");
    }

    @Test
    void validate_validCommand_noViolations() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("testuser")
                .password("testpass")
                .build();

        // when
        final Set<ConstraintViolation<SignInCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void validate_nullUsername_violation() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username(null)
                .password("testpass")
                .build();

        // when
        final Set<ConstraintViolation<SignInCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void validate_blankUsername_violation() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("")
                .password("testpass")
                .build();

        // when
        final Set<ConstraintViolation<SignInCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void validate_nullPassword_violation() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("testuser")
                .password(null)
                .build();

        // when
        final Set<ConstraintViolation<SignInCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void validate_blankPassword_violation() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("testuser")
                .password("  ")
                .build();

        // when
        final Set<ConstraintViolation<SignInCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void validate_bothFieldsNull_multipleViolations() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username(null)
                .password(null)
                .build();

        // when
        final Set<ConstraintViolation<SignInCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
    }
}
