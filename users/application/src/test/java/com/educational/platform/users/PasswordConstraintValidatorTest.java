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
public class PasswordConstraintValidatorTest {

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
    void isValid_nullPassword_isValid() {
        assertThat(sut.isValid(null, context)).isTrue();
    }

    @Test
    void isValid_validPassword_isValid() {
        assertThat(sut.isValid("validPassword", context)).isTrue();
    }

    @Test
    void isValid_tooShortPassword_isInvalid() {
        assertThat(sut.isValid("short", context)).isFalse();
    }

    @Test
    void isValid_passwordWithWhitespace_isInvalid() {
        assertThat(sut.isValid("invalid password", context)).isFalse();
    }
}
