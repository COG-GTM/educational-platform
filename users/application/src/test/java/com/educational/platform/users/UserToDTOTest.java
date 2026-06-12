package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserToDTOTest {

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void toDTO_studentUser_correctFieldsMapped() {
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
        assertThat(dto.username()).isEqualTo("student");
        assertThat(dto.email()).isEqualTo("student@gmail.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toDTO_teacherUser_correctFieldsMapped() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher")
                .email("teacher@edu.com")
                .password("securePass")
                .build();
        when(passwordEncoder.encode("securePass")).thenReturn("hashedPass");
        final User sut = new User(command, passwordEncoder);

        // when
        final UserDTO dto = sut.toDTO();

        // then
        assertThat(dto.username()).isEqualTo("teacher");
        assertThat(dto.email()).isEqualTo("teacher@edu.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toDTO_passwordNotIncluded() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("student")
                .email("student@gmail.com")
                .password("secretPassword")
                .build();
        when(passwordEncoder.encode("secretPassword")).thenReturn("encodedSecret");
        final User sut = new User(command, passwordEncoder);

        // when
        final UserDTO dto = sut.toDTO();

        // then
        assertThat(dto).hasNoNullFieldsOrProperties();
        assertThat(dto.toString()).doesNotContain("encodedSecret");
        assertThat(dto.toString()).doesNotContain("secretPassword");
    }
}
