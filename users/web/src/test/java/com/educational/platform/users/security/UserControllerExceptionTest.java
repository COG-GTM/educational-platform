package com.educational.platform.users.security;

import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.login.SignInCommandHandler;
import com.educational.platform.users.registration.UserRegistrationCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserControllerExceptionTest {

    @Mock
    private UserRegistrationCommandHandler registrationHandler;

    @Mock
    private SignInCommandHandler signInHandler;

    private UserController sut;

    @BeforeEach
    void setUp() {
        sut = new UserController(registrationHandler, signInHandler);
    }

    @Test
    void signUp_handlerThrowsUnprocessableEntity_propagatesException() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "existing", "email@test.com", "password");
        when(registrationHandler.handle(any())).thenThrow(new UnprocessableEntityException("Username: [existing] is already in use"));

        // when / then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(() -> sut.signUp(request))
                .withMessageContaining("existing");
    }

    @Test
    void signUp_handlerThrowsConstraintViolation_propagatesException() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "email@test.com", "pass");
        when(registrationHandler.handle(any())).thenThrow(new ConstraintViolationException(Set.of()));

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.signUp(request));
    }

    @Test
    void signIn_handlerThrowsUnprocessableEntity_propagatesException() {
        // given
        final SignInRequest request = new SignInRequest("user", "wrong-pass");
        when(signInHandler.handle(any())).thenThrow(new UnprocessableEntityException("Invalid username/password"));

        // when / then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(() -> sut.signIn(request))
                .withMessageContaining("Invalid username/password");
    }

    @Test
    void signIn_handlerThrowsConstraintViolation_propagatesException() {
        // given
        final SignInRequest request = new SignInRequest("", "pass");
        when(signInHandler.handle(any())).thenThrow(new ConstraintViolationException(Set.of()));

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.signIn(request));
    }
}
