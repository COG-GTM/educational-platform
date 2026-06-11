package com.educational.platform.users.login;

import com.educational.platform.users.UserRepository;
import com.educational.platform.users.security.JwtTokenProvider;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Additional validation edge cases for SignInCommandHandler:
 * both fields null and blank string inputs.
 */
@ExtendWith(MockitoExtension.class)
public class SignInCommandHandlerValidationTest {

    @Mock
    private UserRepository repository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    private SignInCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new SignInCommandHandler(jwtTokenProvider, repository, validator, authenticationManager);
    }

    @Test
    void handle_bothFieldsNull_constraintViolationException() {
        // given
        final SignInCommand command = SignInCommand.builder().build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_blankUsername_constraintViolationException() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("  ")
                .password("password")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_blankPassword_constraintViolationException() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("user")
                .password("  ")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }
}
