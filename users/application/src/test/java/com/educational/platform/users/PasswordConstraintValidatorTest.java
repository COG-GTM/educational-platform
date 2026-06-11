package com.educational.platform.users;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PasswordConstraintValidatorTest {

    private PasswordConstraintValidator sut;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        sut = new PasswordConstraintValidator();
    }

    @Test
    void isValid_nullPassword_true() {
        // when
        final boolean result = sut.isValid(null, context);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_validPassword_true() {
        // when
        final boolean result = sut.isValid("validPass", context);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_tooShortPassword_false() {
        // given
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        // when
        final boolean result = sut.isValid("short", context);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_passwordWithWhitespace_false() {
        // given
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        // when
        final boolean result = sut.isValid("password with space", context);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_exactMinLength_true() {
        // when
        final boolean result = sut.isValid("12345678", context);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_exactMaxLength_true() {
        // when
        final boolean result = sut.isValid("123456789012345678901234567890", context);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_exceedsMaxLength_false() {
        // given
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        // when
        final boolean result = sut.isValid("1234567890123456789012345678901", context);

        // then
        assertThat(result).isFalse();
    }
}
