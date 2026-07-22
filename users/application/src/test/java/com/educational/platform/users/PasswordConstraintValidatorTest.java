package com.educational.platform.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PasswordConstraintValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private PasswordConstraintValidator sut;

    @BeforeEach
    void setUp() {
        sut = new PasswordConstraintValidator();
    }

    @Test
    void isValid_nullPassword_valid() {
        // given
        final String password = null;

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_validPassword_valid() {
        // given
        final String password = "password";

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_tooShortPassword_invalid() {
        // given
        final String password = "short";
        configureViolationBuilder();

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
    }

    @Test
    void isValid_passwordWithWhitespace_invalid() {
        // given
        final String password = "password with whitespace";
        configureViolationBuilder();

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
    }

    private void configureViolationBuilder() {
        final ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    }
}
