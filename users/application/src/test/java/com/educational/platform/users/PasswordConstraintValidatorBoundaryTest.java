package com.educational.platform.users;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Boundary-condition tests for {@link PasswordConstraintValidator}.
 * Passay rules: length 8–30, no whitespace.
 */
@ExtendWith(MockitoExtension.class)
public class PasswordConstraintValidatorBoundaryTest {

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private PasswordConstraintValidator sut;

    @BeforeEach
    void setUp() {
        sut = new PasswordConstraintValidator();
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    }

    @Test
    void isValid_exactMinLength8_isValid() {
        assertThat(sut.isValid("abcdefgh", context)).isTrue();
    }

    @Test
    void isValid_exactMaxLength30_isValid() {
        assertThat(sut.isValid("a".repeat(30), context)).isTrue();
    }

    @Test
    void isValid_belowMinLength7_isInvalid() {
        assertThat(sut.isValid("abcdefg", context)).isFalse();
    }

    @Test
    void isValid_aboveMaxLength31_isInvalid() {
        assertThat(sut.isValid("a".repeat(31), context)).isFalse();
    }

    @Test
    void isValid_emptyString_isInvalid() {
        assertThat(sut.isValid("", context)).isFalse();
    }

    @Test
    void isValid_onlyWhitespace_isInvalid() {
        assertThat(sut.isValid("        ", context)).isFalse();
    }

    @Test
    void isValid_leadingWhitespace_isInvalid() {
        assertThat(sut.isValid(" password", context)).isFalse();
    }

    @Test
    void isValid_trailingWhitespace_isInvalid() {
        assertThat(sut.isValid("password ", context)).isFalse();
    }

    @Test
    void isValid_middleWhitespace_isInvalid() {
        assertThat(sut.isValid("pass word", context)).isFalse();
    }

    @Test
    void isValid_tabCharacter_isInvalid() {
        assertThat(sut.isValid("abcdefgh\t", context)).isFalse();
    }
}
