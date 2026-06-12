package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void constructor_validCommand_userCreated() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .build();
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        // when
        final User sut = new User(command, passwordEncoder);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("username", "username")
                .hasFieldOrPropertyWithValue("email", "email@gmail.com")
                .hasFieldOrPropertyWithValue("password", "encodedPassword")
                .hasFieldOrPropertyWithValue("role", Role.ROLE_STUDENT);
    }

    @Test
    void toDTO_studentUser_dtoReturned() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .build();
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        final User sut = new User(command, passwordEncoder);

        // when
        final UserDTO result = sut.toDTO();

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("username", "username")
                .hasFieldOrPropertyWithValue("email", "email@gmail.com")
                .hasFieldOrPropertyWithValue("role", RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toDTO_teacherUser_dtoReturned() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher")
                .email("teacher@gmail.com")
                .password("password")
                .build();
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        final User sut = new User(command, passwordEncoder);

        // when
        final UserDTO result = sut.toDTO();

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("username", "teacher")
                .hasFieldOrPropertyWithValue("email", "teacher@gmail.com")
                .hasFieldOrPropertyWithValue("role", RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toUserDetails_validUser_userDetailsReturned() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .build();
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        final User sut = new User(command, passwordEncoder);

        // when
        final UserDetails result = sut.toUserDetails();

        // then
        assertThat(result.getUsername()).isEqualTo("username");
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.isAccountNonExpired()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isCredentialsNonExpired()).isTrue();
        assertThat(result.isEnabled()).isTrue();
    }
}
