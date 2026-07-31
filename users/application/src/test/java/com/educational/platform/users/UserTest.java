package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class UserTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void constructor_validCommand_uuidAssigned() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final User user = new User(command, passwordEncoder);

        // then
        assertThat(user).extracting("uuid").isNotNull();
    }

    @Test
    void constructor_twoUsers_distinctUuids() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final User first = new User(command, passwordEncoder);
        final User second = new User(command, passwordEncoder);

        // then
        assertThat(first.toDTO().uuid()).isNotEqualTo(second.toDTO().uuid());
    }

    @Test
    void toDTO_uuidCarriedIntoDTO() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto.uuid()).isNotNull();
        assertThat(user).hasFieldOrPropertyWithValue("uuid", dto.uuid());
        assertThat(dto.username()).isEqualTo("username");
        assertThat(dto.email()).isEqualTo("email@gmail.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }
}
