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
class UserToUserDetailsTest {

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void toUserDetails_student_accountNotExpired() {
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
        assertThat(result.isAccountNonExpired()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isCredentialsNonExpired()).isTrue();
        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    void toUserDetails_teacher_correctUsernameAndPassword() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher99")
                .email("teacher@edu.com")
                .password("securePass")
                .build();
        when(passwordEncoder.encode("securePass")).thenReturn("hashedSecurePass");
        final User sut = new User(command, passwordEncoder);

        // when
        final UserDetails result = sut.toUserDetails();

        // then
        assertThat(result.getUsername()).isEqualTo("teacher99");
        assertThat(result.getPassword()).isEqualTo("hashedSecurePass");
    }

    @Test
    void toUserDetails_sameUser_consistentResults() {
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
        final UserDetails result1 = sut.toUserDetails();
        final UserDetails result2 = sut.toUserDetails();

        // then
        assertThat(result1.getUsername()).isEqualTo(result2.getUsername());
        assertThat(result1.getPassword()).isEqualTo(result2.getPassword());
        assertThat(result1.getAuthorities()).hasSameSizeAs(result2.getAuthorities());
    }
}
