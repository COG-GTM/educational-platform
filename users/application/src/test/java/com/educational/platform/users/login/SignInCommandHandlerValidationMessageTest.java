package com.educational.platform.users.login;

import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class SignInCommandHandlerValidationMessageTest {

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
    void handle_authenticationFails_exceptionMessageIsInvalidUsernamePassword() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("user")
                .password("wrongpassword")
                .build();
        doThrow(BadCredentialsException.class).when(authenticationManager).authenticate(any());

        // when
        final UnprocessableEntityException exception = catchThrowableOfType(
                () -> sut.handle(command), UnprocessableEntityException.class);

        // then
        assertThat(exception.getMessage()).isEqualTo("Invalid username/password");
    }

    @Test
    void handle_usernameOnlyWhitespace_constraintViolationException() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("   ")
                .password("validpassword")
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(command));
    }

    @Test
    void handle_passwordOnlyWhitespace_constraintViolationException() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("validuser")
                .password("   ")
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(command));
    }
}
