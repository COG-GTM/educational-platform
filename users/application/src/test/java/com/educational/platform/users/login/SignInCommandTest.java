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

    @Test
    void equality_sameValues_equal() {
        // given
        final SignInCommand command1 = SignInCommand.builder()
                .username("user")
                .password("pass")
                .build();
        final SignInCommand command2 = SignInCommand.builder()
                .username("user")
                .password("pass")
                .build();

        // then
        assertThat(command1).isEqualTo(command2);
        assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
    }

    @Test
    void equality_differentValues_notEqual() {
        // given
        final SignInCommand command1 = SignInCommand.builder()
                .username("user1")
                .password("pass1")
                .build();
        final SignInCommand command2 = SignInCommand.builder()
                .username("user2")
                .password("pass2")
                .build();

        // then
        assertThat(command1).isNotEqualTo(command2);
    }

    @Test
    void toString_containsFieldValues() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("testuser")
                .password("testpass")
                .build();

        // when
        final String str = command.toString();

        // then
        assertThat(str).contains("testuser");
        assertThat(str).contains("testpass");
    }
}
