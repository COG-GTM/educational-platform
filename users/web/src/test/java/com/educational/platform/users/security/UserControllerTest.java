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
public class UserControllerTest {

    @Mock
    private UserRegistrationCommandHandler userRegistrationCommandHandler;

    @Mock
    private SignInCommandHandler signInCommandHandler;

    @InjectMocks
    private UserController sut;

    @Test
    void signUp_validRequest_tokenReturned() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "username", "email@gmail.com", "password");
        when(userRegistrationCommandHandler.handle(any(UserRegistrationCommand.class))).thenReturn("token");

        // when
        final String result = sut.signUp(request);

        // then
        assertThat(result).isEqualTo("token");
        final ArgumentCaptor<UserRegistrationCommand> captor = ArgumentCaptor.forClass(UserRegistrationCommand.class);
        verify(userRegistrationCommandHandler).handle(captor.capture());
        final UserRegistrationCommand command = captor.getValue();
        assertThat(command.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
        assertThat(command.username()).isEqualTo("username");
        assertThat(command.email()).isEqualTo("email@gmail.com");
        assertThat(command.password()).isEqualTo("password");
    }

    @Test
    void signIn_validRequest_tokenReturned() {
        // given
        final SignInRequest request = new SignInRequest("username", "password");
        when(signInCommandHandler.handle(any(SignInCommand.class))).thenReturn("token");

        // when
        final String result = sut.signIn(request);

        // then
        assertThat(result).isEqualTo("token");
        final ArgumentCaptor<SignInCommand> captor = ArgumentCaptor.forClass(SignInCommand.class);
        verify(signInCommandHandler).handle(captor.capture());
        final SignInCommand command = captor.getValue();
        assertThat(command.username()).isEqualTo("username");
        assertThat(command.password()).isEqualTo("password");
    }
}
