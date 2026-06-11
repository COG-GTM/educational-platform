package com.educational.platform.users.registration;

import com.educational.platform.users.RoleDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class UserRegistrationCommandTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void builder_allFieldsSet_commandCreatedCorrectly() {
        // when
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        // then
        assertThat(command.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
        assertThat(command.username()).isEqualTo("testuser");
        assertThat(command.email()).isEqualTo("test@example.com");
        assertThat(command.password()).isEqualTo("password123");
    }

    @Test
    void validate_validStudentCommand_noViolations() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void validate_validTeacherCommand_noViolations() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher")
                .email("teacher@example.com")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void validate_nullRole_violation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(null)
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("role"));
    }

    @Test
    void validate_nullUsername_violation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username(null)
                .email("test@example.com")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void validate_usernameTooShort_violation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("abc")
                .email("test@example.com")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void validate_usernameExactMinLength_noViolation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("abcd")
                .email("test@example.com")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void validate_usernameMaxLength_noViolation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("a".repeat(255))
                .email("test@example.com")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void validate_usernameTooLong_violation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("a".repeat(256))
                .email("test@example.com")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void validate_invalidEmail_violation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("not-an-email")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void validate_nullEmail_violation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email(null)
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void validate_blankEmail_violation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void validate_nullPassword_violation() {
        // given
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
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "1234567", "abc"})
    void validate_passwordTooShort_violation(String password) {
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
    void validate_passwordWithWhitespace_violation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password("pass word123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void validate_allFieldsNull_multipleViolations() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(null)
                .username(null)
                .email(null)
                .password(null)
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void validate_blankUsername_violation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("   ")
                .email("test@example.com")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void validate_blankPassword_violation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password("   ")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void validate_whitespaceOnlyEmail_violation() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("   ")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @ParameterizedTest
    @NullSource
    void validate_nullRole_singleViolation(RoleDTO role) {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(role)
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        // when
        final Set<ConstraintViolation<UserRegistrationCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("role"));
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final UserRegistrationCommand command1 = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("user")
                .email("user@example.com")
                .password("password123")
                .build();
        final UserRegistrationCommand command2 = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("user")
                .email("user@example.com")
                .password("password123")
                .build();

        // then
        assertThat(command1).isEqualTo(command2);
        assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
    }

    @Test
    void equality_differentValues_notEqual() {
        // given
        final UserRegistrationCommand command1 = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("user1")
                .email("user1@example.com")
                .password("password1")
                .build();
        final UserRegistrationCommand command2 = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("user2")
                .email("user2@example.com")
                .password("password2")
                .build();

        // then
        assertThat(command1).isNotEqualTo(command2);
    }

    @Test
    void toString_containsFieldValues() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        // when
        final String str = command.toString();

        // then
        assertThat(str).contains("testuser");
        assertThat(str).contains("test@example.com");
        assertThat(str).contains("ROLE_STUDENT");
    }
}
