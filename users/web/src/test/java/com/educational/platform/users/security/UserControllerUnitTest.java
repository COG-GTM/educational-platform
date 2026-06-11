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
public class UserControllerUnitTest {

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
    void signUp_delegatesToRegistrationHandler() {
        // given
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "user", "email@test.com", "pass");
        when(registrationHandler.handle(any())).thenReturn("token");

        // when
        final String result = sut.signUp(request);

        // then
        assertThat(result).isEqualTo("token");
        final ArgumentCaptor<UserRegistrationCommand> captor = ArgumentCaptor.forClass(UserRegistrationCommand.class);
        verify(registrationHandler).handle(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("user");
        assertThat(captor.getValue().email()).isEqualTo("email@test.com");
        assertThat(captor.getValue().role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void signIn_delegatesToSignInHandler() {
        // given
        final SignInRequest request = new SignInRequest("user", "pass");
        when(signInHandler.handle(any())).thenReturn("token");

        // when
        final String result = sut.signIn(request);

        // then
        assertThat(result).isEqualTo("token");
        final ArgumentCaptor<SignInCommand> captor = ArgumentCaptor.forClass(SignInCommand.class);
        verify(signInHandler).handle(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("user");
        assertThat(captor.getValue().password()).isEqualTo("pass");
    }
}
