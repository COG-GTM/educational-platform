package com.educational.platform.users.security;

import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.login.SignInCommand;
import com.educational.platform.users.login.SignInCommandHandler;
import com.educational.platform.users.registration.UserRegistrationCommand;
import com.educational.platform.users.registration.UserRegistrationCommandHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerSuccessTest {

    @Mock
    private UserRegistrationCommandHandler userRegistrationCommandHandler;

    @Mock
    private SignInCommandHandler signInCommandHandler;

    @InjectMocks
    private UserController sut;

    @Test
    void signUp_validRequest_returnsToken() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "newuser", "user@test.com", "password");
        when(userRegistrationCommandHandler.handle(any(UserRegistrationCommand.class))).thenReturn("jwt-token-123");

        // when
        final String result = sut.signUp(request);

        // then
        assertThat(result).isEqualTo("jwt-token-123");
    }

    @Test
    void signUp_validRequest_commandContainsCorrectFields() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "student1", "student@test.com", "securePass");
        when(userRegistrationCommandHandler.handle(any(UserRegistrationCommand.class))).thenReturn("token");

        // when
        sut.signUp(request);

        // then
        final ArgumentCaptor<UserRegistrationCommand> captor = ArgumentCaptor.forClass(UserRegistrationCommand.class);
        verify(userRegistrationCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("student1");
        assertThat(captor.getValue().email()).isEqualTo("student@test.com");
        assertThat(captor.getValue().password()).isEqualTo("securePass");
        assertThat(captor.getValue().role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void signIn_validRequest_returnsToken() {
        // given
        final SignInRequest request = new SignInRequest("existinguser", "correctpassword");
        when(signInCommandHandler.handle(any(SignInCommand.class))).thenReturn("signin-jwt-token");

        // when
        final String result = sut.signIn(request);

        // then
        assertThat(result).isEqualTo("signin-jwt-token");
    }

    @Test
    void signIn_validRequest_commandContainsCorrectFields() {
        // given
        final SignInRequest request = new SignInRequest("testuser", "testpass");
        when(signInCommandHandler.handle(any(SignInCommand.class))).thenReturn("token");

        // when
        sut.signIn(request);

        // then
        final ArgumentCaptor<SignInCommand> captor = ArgumentCaptor.forClass(SignInCommand.class);
        verify(signInCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("testuser");
        assertThat(captor.getValue().password()).isEqualTo("testpass");
    }
}
