package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordConstraintValidatorTest {

    private PasswordConstraintValidator sut;
    private Validator validator;

    @BeforeEach
    void setUp() {
        sut = new PasswordConstraintValidator();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void isValid_nullPassword_isValid() {
        assertThat(sut.isValid(null, null)).isTrue();
    }

    @Test
    void isValid_passwordWithEightCharacters_isValid() {
        assertThat(sut.isValid("password", null)).isTrue();
    }

    @Test
    void isValid_passwordWithThirtyCharacters_isValid() {
        assertThat(sut.isValid("a".repeat(30), null)).isTrue();
    }

    @Test
    void isValid_passwordShorterThanEightCharacters_isInvalid() {
        assertThat(sut.isValid("short", invalidContext())).isFalse();
    }

    @Test
    void isValid_passwordLongerThanThirtyCharacters_isInvalid() {
        assertThat(sut.isValid("a".repeat(31), invalidContext())).isFalse();
    }

    @Test
    void isValid_passwordWithWhitespace_isInvalid() {
        assertThat(sut.isValid("pass word", invalidContext())).isFalse();
    }

    @Test
    void isValid_passwordConstraintAnnotation_validatesPassword() {
        final var command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("username")
                .email("email@gmail.com")
                .password("short")
                .build();

        assertThat(validator.validate(command).stream())
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }

    private static ConstraintValidatorContext invalidContext() {
        final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        final ConstraintValidatorContext.ConstraintViolationBuilder builder =
                mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        return context;
    }
}
