package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void constructor_validCommand_userCreatedWithEncodedPassword() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(passwordEncoder.encode("password")).thenReturn("encoded password");

        // when
        final User user = new User(command, passwordEncoder);

        // then
        assertThat(user)
                .hasFieldOrPropertyWithValue("username", "username")
                .hasFieldOrPropertyWithValue("email", "email@gmail.com")
                .hasFieldOrPropertyWithValue("password", "encoded password")
                .hasFieldOrPropertyWithValue("role", Role.ROLE_STUDENT);
    }

    @Test
    void toDTO_user_dtoWithUsernameEmailAndRole() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(passwordEncoder.encode("password")).thenReturn("encoded password");
        final User user = new User(command, passwordEncoder);

        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("username", "username")
                .hasFieldOrPropertyWithValue("email", "email@gmail.com")
                .hasFieldOrPropertyWithValue("role", RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toUserDetails_user_userDetailsWithUsernamePasswordAndAuthority() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(passwordEncoder.encode("password")).thenReturn("encoded password");
        final User user = new User(command, passwordEncoder);

        // when
        final UserDetails userDetails = user.toUserDetails();

        // then
        assertThat(userDetails.getUsername()).isEqualTo("username");
        assertThat(userDetails.getPassword()).isEqualTo("encoded password");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next()).isEqualTo(Role.ROLE_STUDENT);
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }
}
