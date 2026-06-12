package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserEdgeCaseTest {

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void constructor_teacherRole_userCreatedWithTeacherRole() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher")
                .email("teacher@gmail.com")
                .password("password")
                .build();
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        // when
        final User sut = new User(command, passwordEncoder);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("username", "teacher")
                .hasFieldOrPropertyWithValue("email", "teacher@gmail.com")
                .hasFieldOrPropertyWithValue("password", "encodedPassword")
                .hasFieldOrPropertyWithValue("role", Role.ROLE_TEACHER);
    }

    @Test
    void toUserDetails_teacherRole_hasTeacherAuthority() {
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
        final UserDetails result = sut.toUserDetails();

        // then
        assertThat(result.getUsername()).isEqualTo("teacher");
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_TEACHER");
    }

    @Test
    void toUserDetails_studentRole_hasStudentAuthority() {
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
        final UserDetails result = sut.toUserDetails();

        // then
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_STUDENT");
    }
}
