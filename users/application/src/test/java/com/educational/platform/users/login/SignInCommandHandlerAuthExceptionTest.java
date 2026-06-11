package com.educational.platform.users.login;

import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.security.JwtTokenProvider;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Tests that {@link SignInCommandHandler} wraps various AuthenticationException subtypes
 * into an {@link UnprocessableEntityException} with the message "Invalid username/password".
 */
@ExtendWith(MockitoExtension.class)
public class SignInCommandHandlerAuthExceptionTest {

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
    void handle_badCredentials_throwsUnprocessableEntityWithExpectedMessage() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("user")
                .password("wrong")
                .build();
        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessage("Invalid username/password");
    }

    @Test
    void handle_internalAuthServiceException_throwsUnprocessableEntityWithExpectedMessage() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("user")
                .password("pass")
                .build();
        doThrow(new InternalAuthenticationServiceException("internal error"))
                .when(authenticationManager).authenticate(any());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessage("Invalid username/password");
    }
}
