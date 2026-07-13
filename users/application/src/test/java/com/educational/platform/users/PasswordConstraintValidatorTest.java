package com.educational.platform.users;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordConstraintValidatorTest {

    private PasswordConstraintValidator sut;

    @BeforeEach
    void setUp() {
        sut = new PasswordConstraintValidator();
    }

    @Test
    void isValid_nullPassword_valid() {
        // when
        final boolean valid = sut.isValid(null, null);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void isValid_compliantPassword_valid() {
        // when
        final boolean valid = sut.isValid("password", null);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void isValid_tooShortPassword_invalidWithViolationMessage() {
        // given
        final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        final ConstraintViolationBuilder builder = mock(ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);

        // when
        final boolean valid = sut.isValid("short", context);

        // then
        assertThat(valid).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(anyString());
        verify(builder).addConstraintViolation();
    }

    @Test
    void isValid_passwordWithWhitespace_invalid() {
        // given
        final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        final ConstraintViolationBuilder builder = mock(ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);

        // when
        final boolean valid = sut.isValid("pass word", context);

        // then
        assertThat(valid).isFalse();
        verify(context).disableDefaultConstraintViolation();
    }
}
