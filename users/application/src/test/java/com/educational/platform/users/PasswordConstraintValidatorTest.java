package com.educational.platform.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class PasswordConstraintValidatorTest {

    // deep stubs cover the context.buildConstraintViolationWithTemplate(..).addConstraintViolation()
    // chain the validator walks only on the invalid branch.
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    private PasswordConstraintValidator sut;

    @BeforeEach
    void setUp() {
        sut = new PasswordConstraintValidator();
    }

    @Test
    void isValid_nullPassword_valid() {
        // given - null is delegated to the @NotNull/@NotEmpty constraints, so the password rules treat it as valid

        // when
        final boolean valid = sut.isValid(null, context);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void isValid_compliantPassword_valid() {
        // when
        final boolean valid = sut.isValid("password", context);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void isValid_minimumLengthPassword_valid() {
        // given - eight characters is the inclusive lower bound of LengthRule(8, 30)

        // when
        final boolean valid = sut.isValid("12345678", context);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void isValid_maximumLengthPassword_valid() {
        // given - thirty characters is the inclusive upper bound of LengthRule(8, 30)
        final String password = "a".repeat(30);

        // when
        final boolean valid = sut.isValid(password, context);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void isValid_emptyPassword_invalid() {
        // given - unlike null (which is delegated), an empty string is validated and its zero length
        // falls below LengthRule(8, 30)'s lower bound

        // when
        final boolean valid = sut.isValid("", context);

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void isValid_tooShortPassword_invalid() {
        // given - seven characters is one below the inclusive lower bound

        // when
        final boolean valid = sut.isValid("1234567", context);

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void isValid_tooLongPassword_invalid() {
        // given - thirty-one characters is one above the inclusive upper bound
        final String password = "a".repeat(31);

        // when
        final boolean valid = sut.isValid(password, context);

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void isValid_passwordContainingWhitespace_invalid() {
        // given - WhitespaceRule rejects any whitespace even when the length is within bounds

        // when
        final boolean valid = sut.isValid("pass word", context);

        // then
        assertThat(valid).isFalse();
    }
}
