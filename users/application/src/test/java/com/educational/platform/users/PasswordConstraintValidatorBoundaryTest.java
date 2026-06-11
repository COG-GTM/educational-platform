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
        // 8 characters, no whitespace
        assertThat(sut.isValid("abcdefgh", context)).isTrue();
    }

    @Test
    void isValid_exactlyMaxLength_isValid() {
        // 30 characters, no whitespace
        assertThat(sut.isValid("aaaaabbbbbcccccdddddeeeeefffff", context)).isTrue();
    }

    @Test
    void isValid_exceedsMaxLength_isInvalid() {
        // 31 characters
        assertThat(sut.isValid("aaaaabbbbbcccccdddddeeeeefffffg", context)).isFalse();
    }

    @Test
    void isValid_oneCharBelowMin_isInvalid() {
        // 7 characters
        assertThat(sut.isValid("abcdefg", context)).isFalse();
    }

    @Test
    void isValid_emptyString_isInvalid() {
        assertThat(sut.isValid("", context)).isFalse();
    }
}
