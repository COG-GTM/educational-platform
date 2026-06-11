package com.educational.platform.users.security;

import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.login.SignInCommand;
import com.educational.platform.users.login.SignInCommandHandler;
import com.educational.platform.users.registration.UserRegistrationCommand;
import com.educational.platform.users.registration.UserRegistrationCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserRegistrationCommandHandler userRegistrationCommandHandler;

    @Mock
    private SignInCommandHandler signInCommandHandler;

    private UserController sut;

    @BeforeEach
    void setUp() {
        sut = new UserController(userRegistrationCommandHandler, signInCommandHandler);
    }

    @Test
    void signUp_validRequest_delegatesToHandler() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "password123");
        when(userRegistrationCommandHandler.handle(any())).thenReturn("jwt-token");

        // when
        final String result = sut.signUp(request);

        // then
        assertThat(result).isEqualTo("jwt-token");
    }

    @Test
    void signUp_validRequest_commandMappedCorrectly() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_TEACHER, "teacher", "teacher@example.com", "pass1234");
        when(userRegistrationCommandHandler.handle(any())).thenReturn("token");

        // when
        sut.signUp(request);

        // then
        final ArgumentCaptor<UserRegistrationCommand> captor = ArgumentCaptor.forClass(UserRegistrationCommand.class);
        verify(userRegistrationCommandHandler).handle(captor.capture());
        final UserRegistrationCommand command = captor.getValue();
        assertThat(command.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
        assertThat(command.username()).isEqualTo("teacher");
        assertThat(command.email()).isEqualTo("teacher@example.com");
        assertThat(command.password()).isEqualTo("pass1234");
    }

    @Test
    void signIn_validRequest_delegatesToHandler() {
        // given
        final SignInRequest request = new SignInRequest("user", "password123");
        when(signInCommandHandler.handle(any())).thenReturn("jwt-token");

        // when
        final String result = sut.signIn(request);

        // then
        assertThat(result).isEqualTo("jwt-token");
    }

    @Test
    void signIn_validRequest_commandMappedCorrectly() {
        // given
        final SignInRequest request = new SignInRequest("myuser", "mypass");
        when(signInCommandHandler.handle(any())).thenReturn("token");

        // when
        sut.signIn(request);

        // then
        final ArgumentCaptor<SignInCommand> captor = ArgumentCaptor.forClass(SignInCommand.class);
        verify(signInCommandHandler).handle(captor.capture());
        final SignInCommand command = captor.getValue();
        assertThat(command.username()).isEqualTo("myuser");
        assertThat(command.password()).isEqualTo("mypass");
    }

    @Test
    void signUp_studentRole_roleMappedCorrectly() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "student", "student@example.com", "password");
        when(userRegistrationCommandHandler.handle(any())).thenReturn("token");

        // when
        sut.signUp(request);

        // then
        final ArgumentCaptor<UserRegistrationCommand> captor = ArgumentCaptor.forClass(UserRegistrationCommand.class);
        verify(userRegistrationCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void signUp_handlerThrowsException_propagates() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "password123");
        when(userRegistrationCommandHandler.handle(any())).thenThrow(new RuntimeException("Registration failed"));

        // when / then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> sut.signUp(request))
                .withMessageContaining("Registration failed");
    }

    @Test
    void signIn_handlerThrowsException_propagates() {
        // given
        final SignInRequest request = new SignInRequest("user", "password");
        when(signInCommandHandler.handle(any())).thenThrow(new RuntimeException("Authentication failed"));

        // when / then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> sut.signIn(request))
                .withMessageContaining("Authentication failed");
    }

    @Test
    void signUp_returnsNonNullToken() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "password123");
        when(userRegistrationCommandHandler.handle(any())).thenReturn("non-null-token");

        // when
        final String result = sut.signUp(request);

        // then
        assertThat(result).isNotNull().isNotBlank();
    }

    @Test
    void signIn_returnsNonNullToken() {
        // given
        final SignInRequest request = new SignInRequest("user", "password123");
        when(signInCommandHandler.handle(any())).thenReturn("non-null-token");

        // when
        final String result = sut.signIn(request);

        // then
        assertThat(result).isNotNull().isNotBlank();
    }

    @Test
    void signUp_validRequest_registrationHandlerCalledExactlyOnce() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "password123");
        when(userRegistrationCommandHandler.handle(any())).thenReturn("token");

        // when
        sut.signUp(request);

        // then
        verify(userRegistrationCommandHandler, times(1)).handle(any());
        verify(signInCommandHandler, never()).handle(any());
    }

    @Test
    void signIn_validRequest_signInHandlerCalledExactlyOnce() {
        // given
        final SignInRequest request = new SignInRequest("user", "password123");
        when(signInCommandHandler.handle(any())).thenReturn("token");

        // when
        sut.signIn(request);

        // then
        verify(signInCommandHandler, times(1)).handle(any());
        verify(userRegistrationCommandHandler, never()).handle(any());
    }

    @Test
    void signUp_emailFieldMappedFromRequest() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "specific-email@example.com", "password123");
        when(userRegistrationCommandHandler.handle(any())).thenReturn("token");

        // when
        sut.signUp(request);

        // then
        final ArgumentCaptor<UserRegistrationCommand> captor = ArgumentCaptor.forClass(UserRegistrationCommand.class);
        verify(userRegistrationCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("specific-email@example.com");
    }

    @Test
    void signUp_passwordFieldMappedFromRequest() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "specific-password");
        when(userRegistrationCommandHandler.handle(any())).thenReturn("token");

        // when
        sut.signUp(request);

        // then
        final ArgumentCaptor<UserRegistrationCommand> captor = ArgumentCaptor.forClass(UserRegistrationCommand.class);
        verify(userRegistrationCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().password()).isEqualTo("specific-password");
    }

    @Test
    void signIn_passwordFieldMappedFromRequest() {
        // given
        final SignInRequest request = new SignInRequest("user", "specific-password");
        when(signInCommandHandler.handle(any())).thenReturn("token");

        // when
        sut.signIn(request);

        // then
        final ArgumentCaptor<SignInCommand> captor = ArgumentCaptor.forClass(SignInCommand.class);
        verify(signInCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().password()).isEqualTo("specific-password");
    }

    @Test
    void signUp_handlerThrowsConstraintViolationException_propagates() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "password123");
        when(userRegistrationCommandHandler.handle(any())).thenThrow(new ConstraintViolationException(Collections.emptySet()));

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.signUp(request));
    }

    @Test
    void signUp_handlerThrowsUnprocessableEntityException_propagates() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "user@example.com", "password123");
        when(userRegistrationCommandHandler.handle(any())).thenThrow(new UnprocessableEntityException("Username already in use"));

        // when / then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(() -> sut.signUp(request))
                .withMessageContaining("Username already in use");
    }

    @Test
    void signIn_handlerThrowsConstraintViolationException_propagates() {
        // given
        final SignInRequest request = new SignInRequest("user", "password");
        when(signInCommandHandler.handle(any())).thenThrow(new ConstraintViolationException(Collections.emptySet()));

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.signIn(request));
    }

    @Test
    void signIn_handlerThrowsUnprocessableEntityException_propagates() {
        // given
        final SignInRequest request = new SignInRequest("user", "password");
        when(signInCommandHandler.handle(any())).thenThrow(new UnprocessableEntityException("Invalid username/password"));

        // when / then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(() -> sut.signIn(request))
                .withMessageContaining("Invalid username/password");
    }
}
