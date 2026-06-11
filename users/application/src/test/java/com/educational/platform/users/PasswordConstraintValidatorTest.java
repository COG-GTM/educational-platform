package com.educational.platform.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PasswordConstraintValidatorTest {

    private PasswordConstraintValidator sut;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        sut = new PasswordConstraintValidator();
        context = mock(ConstraintValidatorContext.class);
        final ConstraintValidatorContext.ConstraintViolationBuilder builder =
                mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    }

    @Test
    void isValid_nullPassword_valid() {
        // when
        final boolean result = sut.isValid(null, context);

        // then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"password", "12345678", "abcdefgh", "aBcDeFgH1234"})
    void isValid_validPasswords_valid(String password) {
        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "1234567", "abc"})
    void isValid_tooShortPasswords_invalid(String password) {
        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_tooLongPassword_invalid() {
        // given
        final String password = "a".repeat(31);

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_passwordWithWhitespace_invalid() {
        // given
        final String password = "pass word1";

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_exactMinLength_valid() {
        // given
        final String password = "a".repeat(8);

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_exactMaxLength_valid() {
        // given
        final String password = "a".repeat(30);

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_emptyPassword_invalid() {
        // when
        final boolean result = sut.isValid("", context);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_passwordWithTab_invalid() {
        // given
        final String password = "pass\tword1";

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_passwordWithLeadingSpace_invalid() {
        // given
        final String password = " password1";

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_passwordWithTrailingSpace_invalid() {
        // given
        final String password = "password1 ";

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_oneBelowMinLength_invalid() {
        // given
        final String password = "a".repeat(7);

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_invalidPassword_contextViolationSet() {
        // given
        final String password = "short";

        // when
        sut.isValid(password, context);

        // then
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void isValid_passwordWithNewline_invalid() {
        // given
        final String password = "pass\nword1";

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_oneAboveMaxLength_invalid() {
        // given
        final String password = "a".repeat(31);

        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_validPassword_contextViolationNotSet() {
        // given
        final String password = "validpassword";

        // when
        sut.isValid(password, context);

        // then
        verify(context, never()).disableDefaultConstraintViolation();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"P@ssw0rd!", "abcd1234", "ABCDEFGH", "12345678901234567890123456789a"})
    void isValid_variousValidPasswords_valid(String password) {
        // when
        final boolean result = sut.isValid(password, context);

        // then
        assertThat(result).isTrue();
    }
}
