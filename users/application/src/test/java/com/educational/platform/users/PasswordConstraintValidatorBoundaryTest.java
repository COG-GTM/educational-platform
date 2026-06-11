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
 * Boundary tests for the LengthRule(8, 30) and WhitespaceRule in PasswordConstraintValidator.
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
    void isValid_exactlyMinLength_isValid() {
        // 8 characters — minimum valid length
        assertThat(sut.isValid("abcdefgh", context)).isTrue();
    }

    @Test
    void isValid_exactlyMaxLength_isValid() {
        // 30 characters — maximum valid length
        assertThat(sut.isValid("aaaaabbbbbcccccdddddeeeeefffff", context)).isTrue();
    }

    @Test
    void isValid_oneBelowMinLength_isInvalid() {
        // 7 characters — one below minimum
        assertThat(sut.isValid("abcdefg", context)).isFalse();
    }

    @Test
    void isValid_oneAboveMaxLength_isInvalid() {
        // 31 characters — one above maximum
        assertThat(sut.isValid("aaaaabbbbbcccccdddddeeeeefffffg", context)).isFalse();
    }

    @Test
    void isValid_emptyString_isInvalid() {
        assertThat(sut.isValid("", context)).isFalse();
    }

    @Test
    void isValid_tabCharacter_isInvalid() {
        assertThat(sut.isValid("abcdefgh\t", context)).isFalse();
    }
}
