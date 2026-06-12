package com.educational.platform.users.security;

import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.login.SignInCommand;
import com.educational.platform.users.login.SignInCommandHandler;
import com.educational.platform.users.registration.UserRegistrationCommand;
import com.educational.platform.users.registration.UserRegistrationCommandHandler;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerEdgeCaseTest {

    @Mock
    private UserRegistrationCommandHandler userRegistrationCommandHandler;

    @Mock
    private SignInCommandHandler signInCommandHandler;

    @InjectMocks
    private UserController sut;

    @Test
    void signUp_handlerThrowsUnprocessableEntity_exceptionPropagated() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "existing", "email@test.com", "password");
        when(userRegistrationCommandHandler.handle(any(UserRegistrationCommand.class)))
                .thenThrow(new UnprocessableEntityException("Username: [existing] is already in use"));

        // when
        final ThrowableAssert.ThrowingCallable signUp = () -> sut.signUp(request);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class).isThrownBy(signUp);
    }

    @Test
    void signUp_handlerThrowsConstraintViolation_exceptionPropagated() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "email@test.com", "password");
        when(userRegistrationCommandHandler.handle(any(UserRegistrationCommand.class)))
                .thenThrow(new ConstraintViolationException(Set.of()));

        // when
        final ThrowableAssert.ThrowingCallable signUp = () -> sut.signUp(request);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(signUp);
    }

    @Test
    void signIn_handlerThrowsUnprocessableEntity_exceptionPropagated() {
        // given
        final SignInRequest request = new SignInRequest("user", "wrongpassword");
        when(signInCommandHandler.handle(any(SignInCommand.class)))
                .thenThrow(new UnprocessableEntityException("Invalid username/password"));

        // when
        final ThrowableAssert.ThrowingCallable signIn = () -> sut.signIn(request);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class).isThrownBy(signIn);
    }

    @Test
    void signUp_teacherRole_commandContainsTeacherRole() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_TEACHER, "teacher", "teacher@test.com", "password");
        when(userRegistrationCommandHandler.handle(any(UserRegistrationCommand.class))).thenReturn("token");

        // when
        sut.signUp(request);

        // then
        final ArgumentCaptor<UserRegistrationCommand> captor = ArgumentCaptor.forClass(UserRegistrationCommand.class);
        verify(userRegistrationCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void signUp_noInteractionWithSignInHandler() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "email@test.com", "password");
        when(userRegistrationCommandHandler.handle(any(UserRegistrationCommand.class))).thenReturn("token");

        // when
        sut.signUp(request);

        // then
        verifyNoInteractions(signInCommandHandler);
    }

    @Test
    void signIn_noInteractionWithRegistrationHandler() {
        // given
        final SignInRequest request = new SignInRequest("user", "password");
        when(signInCommandHandler.handle(any(SignInCommand.class))).thenReturn("token");

        // when
        sut.signIn(request);

        // then
        verifyNoInteractions(userRegistrationCommandHandler);
    }
}
