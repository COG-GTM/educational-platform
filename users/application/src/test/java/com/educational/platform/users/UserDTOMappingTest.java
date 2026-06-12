package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDTOMappingTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void toDTO_studentRole_correctMapping() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("student")
                .email("student@gmail.com")
                .password("password")
                .build();
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        final User sut = new User(command, passwordEncoder);

        // when
        final UserDTO dto = sut.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("username", "student")
                .hasFieldOrPropertyWithValue("email", "student@gmail.com")
                .hasFieldOrPropertyWithValue("role", RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toDTO_teacherRole_correctMapping() {
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
        final UserDTO dto = sut.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("username", "teacher")
                .hasFieldOrPropertyWithValue("email", "teacher@gmail.com")
                .hasFieldOrPropertyWithValue("role", RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toDTO_doesNotExposePassword() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("user")
                .email("user@gmail.com")
                .password("secret")
                .build();
        when(passwordEncoder.encode("secret")).thenReturn("encodedSecret");
        final User sut = new User(command, passwordEncoder);

        // when
        final UserDTO dto = sut.toDTO();

        // then
        assertThat(dto.username()).isEqualTo("user");
        assertThat(dto.email()).isEqualTo("user@gmail.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }
}
