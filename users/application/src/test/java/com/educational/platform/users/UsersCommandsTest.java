package com.educational.platform.users;

import com.educational.platform.users.login.SignInCommand;
import com.educational.platform.users.registration.UserRegistrationCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UsersCommandsTest {

    @Test
    void signInCommand_builderBuildsAllFields() {
        // when
        final SignInCommand command = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();

        // then
        assertThat(command.username()).isEqualTo("username");
        assertThat(command.password()).isEqualTo("password");
    }

    @Test
    void userRegistrationCommand_builderBuildsAllFields() {
        // when
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .build();

        // then
        assertThat(command.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
        assertThat(command.username()).isEqualTo("username");
        assertThat(command.email()).isEqualTo("email@gmail.com");
        assertThat(command.password()).isEqualTo("password");
    }
}
