package com.educational.platform.users.security;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
